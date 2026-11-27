/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.detection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import org.wildfly.a2a.test.integration.rest.RestApplication;

/**
 * Verifies that a deployment providing only an {@code @ExtendedAgentCard}, and its {@code AgentExecutor} as a lambda, is
 * recognised as an A2A server deployment.
 *
 * The deployment needs a JAX-RS application of its own, as the A2A resources are only added to JAX-RS deployments.
 */
@ArquillianTest
@RunAsClient
public class ExtendedAgentCardOnlyTestCase {

    @Deployment
    public static WebArchive createArchive() {
        return ShrinkWrap.create(WebArchive.class, "extended.war")
                .addClasses(RestApplication.class, ExtendedAgentCardProducer.class, LambdaAgentExecutorProducer.class)
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsResource("META-INF/microprofile-config.properties")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml");
    }

    /**
     * The SDK does not serve the extended agent card on the public well-known endpoint, so the deployment is checked by
     * sending a message, which the lambda agent executor completes.
     */
    @Test
    public void messageIsHandled() throws Exception {
        String message = """
                {"message": {"messageId": "message-1", "role": "ROLE_USER", "parts": [{"text": "hello"}]}}""";
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:8080/extended/message:send"))
                        .header("Content-Type", "application/json")
                        .header("A2A-Version", "1.0")
                        .POST(HttpRequest.BodyPublishers.ofString(message))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().contains("TASK_STATE_COMPLETED"), response.body());
    }
}
