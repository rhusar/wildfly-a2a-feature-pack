/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.jsonrpc;

import io.a2a.client.ClientBuilder;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
/**
 * Integration test for A2A JSON-RPC transport on WildFly with the A2A feature pack.
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

    @Deployment
    public static WebArchive createTestArchive() {
        return ShrinkWrap.create(WebArchive.class, "ROOT.war")
                // Test utilities from a2a-java-sdk-tests-server-common (test-jar classes)
                .addPackage(AbstractA2AServerTest.class.getPackage())
                // Test classes for this module
                .addPackage(A2AJsonRpcTestCase.class.getPackage())
                // Deployment descriptors
                .addAsManifestResource("META-INF/beans.xml", "beans.xml")
                .addAsWebInfResource("WEB-INF/web.xml", "web.xml")
                // Test properties for AgentCardProducer
                .addAsResource("a2a-requesthandler-test.properties");
    }

}
