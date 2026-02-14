package org.wildfly.a2a.test.integration.grpc;

import jakarta.enterprise.context.ApplicationScoped;

import org.wildfly.extras.a2a.server.apps.grpc.GrpcBeanInitializer;
import org.wildfly.extras.a2a.server.apps.grpc.WildFlyGrpcHandler;

/**
 * Test class that extends WildFlyGrpcHandler to verify deployment-based gRPC handler inheritance.
 */
@ApplicationScoped
public class DeploymentGrpcBeanInitializer extends WildFlyGrpcHandler {
}
