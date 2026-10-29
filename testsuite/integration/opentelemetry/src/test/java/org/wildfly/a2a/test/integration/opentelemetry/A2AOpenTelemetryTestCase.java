/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.opentelemetry;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.ClientTransportWrapper;
import org.a2aproject.sdk.spec.AgentCard;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

/**
 * Integration test for the {@code a2a-server-opentelemetry} and {@code a2a-client-opentelemetry}
 * layers.
 *
 * <p>The server side is covered end to end: the deployment turns MicroProfile Telemetry on with the
 * logging span exporter, the test sends a message over JSON-RPC and then asserts the span that
 * {@code OpenTelemetryRequestHandlerDecorator} wraps around {@code onMessageSend} reaches the server
 * log. That exercises the whole chain - the module is visible to the deployment, the decorator is a
 * bean, its {@code Tracer} injection point is satisfied by MicroProfile Telemetry, and
 * {@code @Priority} enables it for the deployment's {@code RequestHandler}.
 *
 * <p>The client side is covered as a provisioning test. Its two wrappers are {@code ServiceLoader}
 * services that stay dormant unless the application puts a {@code Tracer} (respectively an
 * {@code OpenTelemetry}) into the transport config, so what the layer has to get right is that the
 * deployment can see the modules and their services at all.
 */
@ArquillianTest
public class A2AOpenTelemetryTestCase {

    /**
     * The span {@code OpenTelemetryRequestHandlerDecorator} opens around {@code onMessageSend}; the
     * name is {@code A2AMethods.SEND_MESSAGE_METHOD}.
     */
    private static final String SEND_MESSAGE_SPAN = "SendMessage";

    private static final String OPERATION_NAME_ATTRIBUTE = "gen_ai.agent.a2a.operation.name";

    /**
     * The logging exporter flushes on {@code otel.bsp.schedule.delay}, set to 100ms by the
     * deployment; this is slack for a loaded CI machine, not an expected wait.
     */
    private static final long SPAN_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "ROOT.war")
                .addPackage(A2AOpenTelemetryTestCase.class.getPackage())
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsResource("META-INF/microprofile-config.properties")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml");
    }

    /**
     * The {@code a2a-client-opentelemetry} modules reach the deployment, and their
     * {@code ClientTransportWrapper} services with them - the subsystem imports services from the
     * modules it adds, so a transport built in the deployment picks the wrappers up.
     */
    @Test
    public void clientTransportWrappersAreAvailable() {
        List<String> wrappers = ServiceLoader.load(ClientTransportWrapper.class,
                        A2AOpenTelemetryTestCase.class.getClassLoader()).stream()
                .map(provider -> provider.type().getName())
                .collect(Collectors.toList());

        assertTrue(wrappers.contains(
                        "org.a2aproject.sdk.extras.opentelemetry.client.OpenTelemetryClientTransportWrapper"),
                () -> "tracing wrapper not registered, found " + wrappers);
        assertTrue(wrappers.contains(
                        "org.a2aproject.sdk.extras.opentelemetry.client.propagation"
                                + ".OpenTelemetryClientPropagatorTransportWrapper"),
                () -> "propagation wrapper not registered, found " + wrappers);
    }

    /**
     * Sends a message to the agent and waits for the server-side span to be exported to the log.
     */
    @Test
    @RunAsClient
    public void serverRequestIsTraced() throws Exception {
        sendMessage();

        String span = awaitSpan(serverLog());
        assertTrue(span.contains(OPERATION_NAME_ATTRIBUTE + "=" + SEND_MESSAGE_SPAN),
                () -> "span carries no A2A operation name: " + span);
        assertTrue(span.contains(" SERVER "), () -> "span is not a server span: " + span);
    }

    private static void sendMessage() throws Exception {
        AgentCard card = A2A.getAgentCard(EchoAgentProducer.AGENT_URL);
        CompletableFuture<Void> responded = new CompletableFuture<>();
        try (Client client = Client.builder(card)
                .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder())
                .build()) {
            client.sendMessage(A2A.toUserMessage("trace me"),
                    List.of((event, agentCard) -> responded.complete(null)),
                    responded::completeExceptionally,
                    null);
            responded.get(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Polls the server log for the exported span. The logging exporter writes one line per span,
     * {@code 'SendMessage' : <trace id> <span id> SERVER [...] AttributesMap{...}}.
     */
    private static String awaitSpan(Path log) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + SPAN_TIMEOUT_MS;
        do {
            if (Files.exists(log)) {
                try (Stream<String> lines = Files.lines(log, StandardCharsets.UTF_8)) {
                    String span = lines
                            .filter(line -> line.contains("'" + SEND_MESSAGE_SPAN + "'"))
                            .findFirst()
                            .orElse(null);
                    if (span != null) {
                        return span;
                    }
                }
            }
            TimeUnit.MILLISECONDS.sleep(250);
        } while (System.currentTimeMillis() < deadline);

        return fail("No '" + SEND_MESSAGE_SPAN + "' span was exported to " + log
                + " within " + SPAN_TIMEOUT_MS + "ms");
    }

    private static Path serverLog() {
        String jbossHome = System.getProperty("jboss.dist");
        if (jbossHome == null) {
            throw new IllegalStateException("jboss.dist is not set");
        }
        return Path.of(jbossHome, "standalone", "log", "server.log");
    }
}
