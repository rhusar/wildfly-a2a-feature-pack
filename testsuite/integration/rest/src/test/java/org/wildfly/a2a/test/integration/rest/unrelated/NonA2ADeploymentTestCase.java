/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.unrelated;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;
import org.wildfly.a2a.test.integration.rest.A2ARestTestCase;

/**
 * Verifies that the A2A subsystem leaves deployments that do not use A2A alone, while an A2A deployment runs next to them.
 *
 * The deployment that does not use A2A is a CDI deployment with {@code bean-discovery-mode="all"} and a JAX-RS application,
 * which would pick up the A2A beans, resources and routing filters if the A2A modules were added to it.
 */
@ArquillianTest
@RunAsClient
public class NonA2ADeploymentTestCase {

    private static final String A2A_URL = "http://localhost:8080";
    private static final String PLAIN_URL = "http://localhost:8080/plain";

    private final HttpClient client = HttpClient.newHttpClient();

    @Deployment(name = "a2a", order = 1)
    public static WebArchive createA2AArchive() throws Exception {
        return A2ARestTestCase.createTestArchive();
    }

    @Deployment(name = "plain", order = 2)
    public static WebArchive createPlainArchive() {
        return ShrinkWrap.create(WebArchive.class, "plain.war")
                .addClasses(PlainApplication.class, PlainBean.class, PlainResource.class)
                .addAsWebInfResource("META-INF/beans.xml", "beans.xml");
    }

    @Test
    public void plainDeploymentWorks() throws Exception {
        HttpResponse<String> response = get(PLAIN_URL + "/plain");
        assertEquals(200, response.statusCode());
        assertEquals("ok", response.body());
    }

    @Test
    public void plainDeploymentDoesNotExposeA2AEndpoints() throws Exception {
        assertEquals(404, get(PLAIN_URL + "/.well-known/agent-card.json").statusCode());
    }

    @Test
    public void a2aDeploymentExposesA2AEndpoints() throws Exception {
        assertEquals(200, get(A2A_URL + "/.well-known/agent-card.json").statusCode());
    }

    @Test
    public void plainDeploymentDoesNotSeeA2AServerModules() throws Exception {
        assertVisible("org.wildfly.a2a.jakarta.rest.A2ARestServerResource", false);
        assertVisible("org.wildfly.a2a.jakarta.common.AgentCardRoutingFilter", false);
        assertVisible("org.a2aproject.sdk.server.PublicAgentCard", false);
        assertVisible("org.wildfly.extension.a2a.cdi.A2ACdiExtension", false);
    }

    @Test
    public void plainDeploymentDoesNotSeeThirdPartyLibraries() throws Exception {
        assertVisible("com.google.common.collect.ImmutableList", false);
        assertVisible("com.google.gson.Gson", false);
        assertVisible("com.google.protobuf.Message", false);
    }

    @Test
    public void plainDeploymentDoesNotSeeA2AClientModules() throws Exception {
        assertVisible("org.a2aproject.sdk.spec.AgentCard", false);
        assertVisible("org.a2aproject.sdk.client.Client", false);
    }

    private void assertVisible(String className, boolean expected) throws IOException, InterruptedException {
        HttpResponse<String> response = get(PLAIN_URL + "/plain/visible?class=" + className);
        assertEquals(200, response.statusCode());
        assertEquals(String.valueOf(expected), response.body(), className);
    }

    private HttpResponse<String> get(String url) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
