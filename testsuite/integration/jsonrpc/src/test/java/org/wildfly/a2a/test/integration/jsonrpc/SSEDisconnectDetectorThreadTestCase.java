/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

/**
 * Verifies that the {@code sse-disconnect-detector} thread does not outlive the deployment that started it.
 *
 * <p>The thread belongs to http-common's {@code @ApplicationScoped SSEHeartbeatScheduler}, which every streaming
 * resource uses to notice a client that has gone away, and is stopped by {@code SSESubscriberLifecycleListener}.
 * That listener is a {@code @WebListener} in a JBoss module, so WildFly - which scans only the deployment's own
 * classes for servlet annotations - never registers it by itself; {@code A2AWebListenerProcessor} adds it to the
 * web metadata instead. Without that the thread leaked, one per deployment that ever served a streaming request.
 *
 * <p>Thread names are read from the server over the management API rather than in-container, because what matters
 * happens after the deployment is gone.
 *
 * @see <a href="https://github.com/rhusar/wildfly-a2a-feature-pack/issues/135">issue #135</a>
 */
@ArquillianTest
@RunAsClient
public class SSEDisconnectDetectorThreadTestCase {

    private static final String DEPLOYMENT = "sse-disconnect-detector";

    private static final String DETECTOR_THREAD = "sse-disconnect-detector";

    private static final String STREAMING_REQUEST = """
            {"jsonrpc":"2.0","id":"1","method":"SendStreamingMessage","params":{"message":{\
            "messageId":"message-1","role":"ROLE_USER","parts":[{"text":"tell me a joke"}]}}}""";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private ManagementClient managementClient;

    /**
     * Deployed by hand, so that the test can look at the server once it is gone again. Left testable, because the
     * archive it reuses carries SDK test classes that need the test libraries Arquillian adds.
     */
    @Deployment(name = DEPLOYMENT, managed = false)
    public static WebArchive createTestArchive() {
        return A2AJsonRpcTestCase.createTestArchive();
    }

    @Test
    public void testDetectorThreadIsStoppedOnUndeploy() throws Exception {
        // Other test classes share the server, and with the listener registered they leave nothing behind - but
        // count rather than assume, so that a leak elsewhere shows up as this test's own leak
        int baseline = detectorThreads();

        deployer.deploy(DEPLOYMENT);
        try {
            sendStreamingRequest();
            assertEquals(baseline + 1, detectorThreads(),
                    "a streaming request should have started the disconnect detector");
        } finally {
            deployer.undeploy(DEPLOYMENT);
        }

        assertEquals(baseline, awaitDetectorThreads(baseline),
                "SSESubscriberLifecycleListener should have stopped the disconnect detector at undeploy");
    }

    private void sendStreamingRequest() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(managementClient.getWebUri().toString()))
                .header("Content-Type", "application/json")
                // Unversioned requests are routed to v0.3, which this server does not provision
                .header("A2A-Version", "1.0")
                .POST(HttpRequest.BodyPublishers.ofString(STREAMING_REQUEST))
                .timeout(TIMEOUT)
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        assertTrue(response.body().startsWith("data:"), response.body());
    }

    /**
     * Waits for the detector thread count to drop to {@code expected}. {@code shutdownNow()} interrupts the thread,
     * so it is gone shortly after the deployment stops rather than immediately.
     */
    private int awaitDetectorThreads(int expected) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + TIMEOUT.toNanos();
        int threads;
        while ((threads = detectorThreads()) != expected && System.nanoTime() < deadline) {
            Thread.sleep(250);
        }
        return threads;
    }

    private int detectorThreads() throws IOException {
        ModelNode operation = new ModelNode();
        operation.get("operation").set("dump-all-threads");
        operation.get("address").add("core-service", "platform-mbean").add("type", "threading");
        operation.get("locked-monitors").set(false);
        operation.get("locked-synchronizers").set(false);

        ModelNode result = managementClient.getControllerClient().execute(operation);
        assertEquals("success", result.get("outcome").asString(), result.toString());

        int threads = 0;
        for (ModelNode thread : result.get("result").asList()) {
            if (DETECTOR_THREAD.equals(thread.get("thread-name").asString())) {
                threads++;
            }
        }
        return threads;
    }
}
