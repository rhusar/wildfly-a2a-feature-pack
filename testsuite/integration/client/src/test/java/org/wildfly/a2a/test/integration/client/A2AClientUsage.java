/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.client;

import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.ClientBuilder;
import org.a2aproject.sdk.client.config.ClientConfig;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.rest.RestTransport;

/**
 * Carries the client API types into the deployment's constant pool, which is what WildFly Glow
 * matches its {@code org.wildfly.rule.class} rules against, and what the A2A subsystem checks to
 * recognise an A2A client deployment. Without it the deployment references no A2A type at all -
 * {@link A2AClientProvisioningTestCase} names its classes as strings - so Glow would discover no
 * A2A layer to scan and the subsystem would add no A2A modules to the deployment.
 *
 * <p>Only this class is added to the archive, not the SDK jars, so the types still resolve against
 * the modules the subsystem adds rather than against anything the deployment ships.
 */
@SuppressWarnings("unused")
public class A2AClientUsage {

    Client client;
    ClientBuilder clientBuilder;
    ClientConfig clientConfig;
    JSONRPCTransport jsonrpcTransport;
    RestTransport restTransport;
    GrpcTransport grpcTransport;
}
