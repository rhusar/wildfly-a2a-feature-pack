/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ServiceLoader;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.jupiter.api.Test;

/**
 * Verifies what a client-only provisioning - the three {@code a2a-client-*} layers without any
 * {@code a2a-server-*} layer - makes available to a deployment.
 *
 * <p>This is a provisioning test, not a client test: it exercises the layer specs and the resulting
 * module graph, and asserts nothing about A2A client behaviour. The client transports are covered
 * functionally by the jsonrpc, rest and grpc modules, which talk to a server over each of them.
 *
 * <p>The deployment bundles no A2A classes of its own, so every class it can load comes from the
 * modules the A2A subsystem adds to it.
 *
 * <p>This is the coverage the client layers lacked when they were provisioning the server-side
 * transport modules by mistake; see issue #111. Note that it pins the transports specifically:
 * {@code org.a2aproject.sdk.server-common} is still installed on a client-only server, because the
 * {@code org.a2aproject.sdk.microprofile-config} module the base {@code a2a} layer provisions has a
 * hard dependency on it ({@code A2AConfigProvider} lives there).
 */
@ArquillianTest
public class A2AClientProvisioningTestCase {

    /**
     * Client transports, which the {@code a2a-client-*} layers exist to provision.
     */
    private static final String[] CLIENT_TRANSPORT_CLASSES = {
            "org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportProvider",
            "org.a2aproject.sdk.client.transport.grpc.GrpcTransportProvider",
            "org.a2aproject.sdk.client.transport.rest.RestTransportProvider"
    };

    /**
     * Server-side transport handlers, which only the {@code a2a-server-*} layers provision.
     */
    private static final String[] SERVER_TRANSPORT_CLASSES = {
            "org.a2aproject.sdk.transport.jsonrpc.handler.JSONRPCHandler",
            "org.a2aproject.sdk.transport.grpc.handler.GrpcHandler",
            "org.a2aproject.sdk.transport.rest.handler.RestHandler"
    };

    @Deployment
    public static WebArchive createTestArchive() {
        // Deliberately not a bean archive: the server-side beans reachable from the modules the
        // subsystem adds are not all satisfiable without an a2a-server-* layer.
        return ShrinkWrap.create(WebArchive.class, "a2a-client-provisioning.war")
                .addClass(A2AClientProvisioningTestCase.class)
                .addClass(A2AClientUsage.class);
    }

    @Test
    public void clientTransportsAreAvailable() throws Exception {
        for (String className : CLIENT_TRANSPORT_CLASSES) {
            assertNotNull(load(className), className);
        }
    }

    /**
     * The client looks up its transports and HTTP client with {@link ServiceLoader} through the deployment's class loader,
     * so the deployment has to import the services of the modules that provide them.
     */
    @Test
    public void clientProvidersAreDiscoverable() throws Exception {
        assertEquals(CLIENT_TRANSPORT_CLASSES.length, countProviders("org.a2aproject.sdk.client.transport.spi.ClientTransportProvider"));
        assertNotEquals(0, countProviders("org.a2aproject.sdk.client.http.A2AHttpClientProvider"));
    }

    @Test
    public void serverTransportsAreNotProvisioned() {
        for (String className : SERVER_TRANSPORT_CLASSES) {
            assertThrows(ClassNotFoundException.class, () -> load(className), className);
        }
    }

    private static Class<?> load(String className) throws ClassNotFoundException {
        return Class.forName(className, false, A2AClientProvisioningTestCase.class.getClassLoader());
    }

    private static long countProviders(String serviceName) throws ClassNotFoundException {
        return ServiceLoader.load(load(serviceName), A2AClientProvisioningTestCase.class.getClassLoader()).stream().count();
    }
}
