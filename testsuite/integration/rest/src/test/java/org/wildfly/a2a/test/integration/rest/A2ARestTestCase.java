/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest;

import java.io.File;

import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.transport.rest.RestTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransportConfigBuilder;
import org.a2aproject.sdk.server.apps.common.AbstractA2AServerTest;
import org.a2aproject.sdk.server.apps.common.TestTaskAuthorizationProvider;
import org.a2aproject.sdk.spec.TransportProtocol;
import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test for A2A REST (HTTP-JSON) transport on WildFly with the A2A feature-pack.
 *
 * The A2A SDK dependencies are provided by the A2A subsystem modules automatically.
 * Only test-specific classes need to be bundled in the WAR.
 */
@ArquillianTest
@RunAsClient
public class A2ARestTestCase extends AbstractA2AServerTest {

    public A2ARestTestCase() {
        super(8080);
    }

    @Override
    protected String getTransportProtocol() {
        return TransportProtocol.HTTP_JSON.asString();
    }

    @Override
    protected String getTransportUrl() {
        return "http://localhost:8080";
    }

    @Override
    protected void configureTransport(ClientBuilder builder) {
        builder.withTransport(RestTransport.class, new RestTransportConfigBuilder());
    }

    @Deployment
    public static WebArchive createTestArchive() throws Exception {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "ROOT.war")
                // RestAssured library needed by AbstractA2AServerTest
                .addAsLibrary(getJarForClass(RestAssured.class))
                // Test utilities from a2a-java-sdk-tests-server-common
                .addPackage(AbstractA2AServerTest.class.getPackage())
                // Test classes for this module
                .addPackage(A2ARestTestCase.class.getPackage())
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

    static JavaArchive getJarForClass(Class<?> clazz) throws Exception {
        File f = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        return ShrinkWrap.createFromZipFile(JavaArchive.class, f);
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
