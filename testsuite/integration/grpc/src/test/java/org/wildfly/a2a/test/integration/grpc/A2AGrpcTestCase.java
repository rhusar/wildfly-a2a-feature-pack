/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.grpc;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.a2a.server.apps.common.AbstractA2AServerTest;
import io.a2a.client.ClientBuilder;
import io.a2a.client.transport.grpc.GrpcTransport;
import io.a2a.client.transport.grpc.GrpcTransportConfigBuilder;
import io.a2a.spec.TransportProtocol;
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
import org.junit.jupiter.api.AfterAll;

/**
 * Integration test for A2A gRPC transport on WildFly with the A2A feature pack.
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
        return ShrinkWrap.create(WebArchive.class, "ROOT.war")
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
    }

    static JavaArchive getJarForClass(Class<?> clazz) throws Exception {
        File f = new File(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        return ShrinkWrap.createFromZipFile(JavaArchive.class, f);
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
}
