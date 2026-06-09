/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.jsonrpc;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.server.apps.common.TestTaskAuthorizationProvider;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
/**
 * Integration test for A2A JSON-RPC transport on WildFly with the A2A feature-pack.
 *
 * All A2A SDK dependencies are provided by the A2A subsystem modules automatically.
 */
@ArquillianTest
@RunAsClient
public class A2AJsonRpcTestCase extends AbstractA2AServerTest {

    public A2AJsonRpcTestCase() {
        super(8080);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.JSONRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8080";
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfigBuilder());
    }

    /**
     * Disabled in the feature-pack deployment model: this test requires the CDI request context to
     * be active on the SDK agent-executor threads, which the A2A Jakarta layer achieves by enabling
     * a managed-executor {@code @Internal Executor} producer bundled in the application archive.
     * In the feature pack the SDK transports live in JBoss modules whose bean archives cannot see a
     * deployment- or module-contributed alternative executor, so the SDK's default (non
     * context-propagating) executor is always used. Request-scoped propagation onto agent threads is
     * therefore an unsupported scenario here.
     */
    @Test
    @Disabled("Request context propagation onto SDK agent-executor threads is not supported in the modular feature-pack deployment model")
    @Override
    public void testRequestScopedBeanAvailableOnAgentExecutorThread() {
    }

    @Deployment
    public static WebArchive createTestArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ROOT.war")
                // Test utilities from a2a-java-sdk-tests-server-common (test-jar classes)
                .addPackage(AbstractA2AServerTest.class.getPackage())
                // Test classes for this module
                .addPackage(A2AJsonRpcTestCase.class.getPackage())
                // Deployment descriptors
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                // Test properties for AgentCardProducer
                .addAsResource("a2a-requesthandler-test.properties");

        // TestTaskAuthorizationProvider gates itself with Quarkus' @IfBuildProperty, which WildFly does not
        // recognise, so the bean would always be active and deny every unauthenticated request.
        archive.delete("/WEB-INF/classes/"
                + TestTaskAuthorizationProvider.class.getName().replace('.', '/') + ".class");

        return archive;
    }

    /**
     * Request-scoped beans are not available on the agent executor threads when A2A is provided as a feature-pack.
     * a2a-jakarta overrides the SDK's {@code @Internal Executor} with an {@code @Alternative} producer backed by a
     * {@code ManagedExecutorService} ({@code AsyncManagedExecutorServiceProducer}). Here that producer sits in the
     * {@code org.wildfly.a2a.jakarta.common} JBoss module, and the {@code @Alternative} is never selected for the
     * injection point in {@code org.a2aproject.sdk.server-common}, so the SDK's own executor always wins.
     */
    @Test
    @Disabled("Request context propagation to the agent executor threads is not supported by the feature-pack")
    @Override
    public void testRequestScopedBeanAvailableOnAgentExecutorThread() {
    }

    /**
     * @see #testRequestScopedBeanAvailableOnAgentExecutorThread()
     */
    @Test
    @Disabled("Request context propagation to the agent executor threads is not supported by the feature-pack")
    @Override
    public void testRequestScopedBeanAvailableOnAgentExecutorThreadStreaming() {
    }
}
