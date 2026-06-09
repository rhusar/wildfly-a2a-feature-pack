/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.grpc;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.server.apps.common.TestTaskAuthorizationProvider;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.restassured.RestAssured;
import io.restassured.common.mapper.ObjectDeserializationContext;
import io.restassured.mapper.ObjectMapper;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test for A2A gRPC transport on WildFly with the A2A feature-pack.
 *
 * The A2A subsystem registers the WildFlyGrpcHandler with the gRPC subsystem's deployment registry during the INSTALL phase.
 */
@ArquillianTest
@RunAsClient
public class A2AGrpcTestCase extends AbstractA2AServerTest {

    private static ManagedChannel channel;

    public A2AGrpcTestCase() {
        super(8080); // HTTP server port for utility endpoints
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }

    @Override
    protected String getTransportUrl() {
        // gRPC port (from WildFly gRPC configuration)
        return "localhost:9555";
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> {
            channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
            return channel;
        }));
    }

    @Deployment
    public static WebArchive createTestArchive() throws Exception{
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ROOT.war")
                // RestAssured libraries needed by AbstractA2AServerTest
                .addAsLibrary(getJarForClass(RestAssured.class))
                .addAsLibrary(getJarForClass(ObjectDeserializationContext.class))
                // Test utilities from a2a-java-sdk-tests-server-common
                .addPackage(AbstractA2AServerTest.class.getPackage())
                // Test resources for this module
                .addPackage(A2AGrpcTestCase.class.getPackage())
                // Deployment descriptors
                .addAsWebInfResource("META-INF/beans.xml", "beans.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                // Test properties for AgentCardProducer
                .addAsResource("a2a-requesthandler-test.properties");

        // TestTaskAuthorizationProvider gates itself with Quarkus' @IfBuildProperty, which WildFly does not
        // recognise, so the bean would always be active and deny every unauthenticated request.
        archive.delete("/WEB-INF/classes/"
                + TestTaskAuthorizationProvider.class.getName().replace('.', '/') + ".class");

        return archive;
    }

    static JavaArchive getJarForClass(Class<?> clazz) throws Exception {
        File f = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        return ShrinkWrap.createFromZipFile(JavaArchive.class, f);
    }

    @Override
    public void testAgentCardHeaders() {
        // gRPC doesn't use HTTP caching headers for Agent Card
    }

    /**
     * Disabled in the feature-pack deployment model: request-scoped propagation onto the SDK
     * agent-executor threads requires a managed-executor {@code @Internal Executor} bundled in the
     * application archive, which is not visible to the SDK transport modules' bean archives in the
     * modular feature-pack layout. See {@code A2AJsonRpcTestCase} for details.
     */
    @Test
    @Disabled("Request context propagation onto SDK agent-executor threads is not supported in the modular feature-pack deployment model")
    @Override
    public void testRequestScopedBeanAvailableOnAgentExecutorThread() {
    }

    @AfterAll
    public static void closeChannel() {
        if (channel != null) {
            channel.shutdownNow();
            try {
                channel.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
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
