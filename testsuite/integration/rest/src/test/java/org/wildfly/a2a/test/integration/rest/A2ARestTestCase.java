/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest;

import java.io.File;

import io.a2a.client.ClientBuilder;
import io.a2a.client.transport.rest.RestTransport;
import io.a2a.client.transport.rest.RestTransportConfigBuilder;
import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.spec.TransportProtocol;
import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.container.annotation.ArquillianTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Integration test for A2A REST (HTTP-JSON) transport on WildFly with the A2A feature pack.
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
        return ShrinkWrap.create(WebArchive.class, "ROOT.war")
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
    }

    static JavaArchive getJarForClass(Class<?> clazz) throws Exception {
        File f = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        return ShrinkWrap.createFromZipFile(JavaArchive.class, f);
    }
}
