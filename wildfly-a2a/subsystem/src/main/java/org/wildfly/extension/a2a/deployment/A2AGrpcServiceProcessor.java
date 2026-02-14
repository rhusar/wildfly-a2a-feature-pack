/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import java.util.Collections;
import java.util.function.Supplier;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.a2a.A2ALogger;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;

import io.grpc.BindableService;

/**
 * Deployment processor that manually registers the A2A gRPC handler with the WildFly gRPC deployment registry.
 *
 * This bypasses the Jandex annotation scanning approach which has issues with module class visibility.
 * Instead, we directly register the WildFlyGrpcHandler class with the gRPC subsystem's deployment registry.
 */
public class A2AGrpcServiceProcessor implements DeploymentUnitProcessor {

    private static final String GRPC_SERVER_CAPABILITY = "org.wildfly.grpc.server";
    private static final String GRPC_HANDLER_CLASS = "org.wildfly.extras.a2a.server.apps.grpc.WildFlyGrpcHandler";
    private static final String GRPC_HANDLER_MODULE = "org.wildfly.a2a.jakarta.grpc";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        A2ALogger.ROOT_LOGGER.infof("A2AGrpcServiceProcessor running for deployment '%s'", deploymentUnit.getName());

        // Check if gRPC capability is available
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support == null || !support.hasCapability(GRPC_SERVER_CAPABILITY)) {
            A2ALogger.ROOT_LOGGER.infof("gRPC capability not available, skipping A2A gRPC service registration for '%s'",
                    deploymentUnit.getName());
            return;
        }

        // Load WildFlyGrpcHandler class from its module to avoid compile-time dependency
        Class<? extends BindableService> handlerClass;
        try {
            Module module = Module.getBootModuleLoader().loadModule(GRPC_HANDLER_MODULE);
            @SuppressWarnings("unchecked")
            Class<? extends BindableService> clazz = (Class<? extends BindableService>)
                    module.getClassLoader().loadClass(GRPC_HANDLER_CLASS);
            handlerClass = clazz;
        } catch (Exception e) {
            A2ALogger.ROOT_LOGGER.warnf("Failed to load gRPC handler class for '%s': %s",
                    deploymentUnit.getName(), e.getMessage());
            return;
        }

        // Install a service that depends on the gRPC registry and registers our handler
        ServiceName serviceName = support.getCapabilityServiceName(GRPC_SERVER_CAPABILITY);
        ServiceName a2aGrpcServiceName = deploymentUnit.getServiceName().append("a2a-grpc-registration");

        ServiceBuilder<?> builder = phaseContext.getServiceTarget().addService(a2aGrpcServiceName);
        Supplier<WildFlyGrpcDeploymentRegistry> registrySupplier = builder.requires(serviceName);

        builder.setInstance(new A2AGrpcRegistrationService(deploymentUnit, handlerClass, registrySupplier));
        builder.install();

        A2ALogger.ROOT_LOGGER.infof("Installed A2A gRPC registration service for deployment '%s'", deploymentUnit.getName());
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        // The gRPC subsystem handles cleanup via removeDeploymentServices
    }

    /**
     * A small service that registers the A2A gRPC handler when started, allowing us to properly depend on the gRPC registry service.
     */
    private static class A2AGrpcRegistrationService implements Service {
        private final DeploymentUnit deploymentUnit;
        private final Class<? extends BindableService> handlerClass;
        private final Supplier<WildFlyGrpcDeploymentRegistry> registrySupplier;

        A2AGrpcRegistrationService(DeploymentUnit deploymentUnit,
                                   Class<? extends BindableService> handlerClass,
                                   Supplier<WildFlyGrpcDeploymentRegistry> registrySupplier) {
            this.deploymentUnit = deploymentUnit;
            this.handlerClass = handlerClass;
            this.registrySupplier = registrySupplier;
        }

        @Override
        public void start(StartContext context) {
            WildFlyGrpcDeploymentRegistry registry = registrySupplier.get();
            if (registry == null) {
                A2ALogger.ROOT_LOGGER.warnf("gRPC deployment registry is null, skipping A2A gRPC service registration for '%s'",
                        deploymentUnit.getName());
                return;
            }

            A2ALogger.ROOT_LOGGER.infof("Got gRPC registry: %s", registry.getClass().getName());

            try {
                registry.addService(deploymentUnit, handlerClass, Collections.emptyList());
                A2ALogger.ROOT_LOGGER.infof("Registered A2A gRPC handler '%s' for deployment '%s'",
                        GRPC_HANDLER_CLASS, deploymentUnit.getName());
            } catch (Exception e) {
                A2ALogger.ROOT_LOGGER.warnf(e, "Failed to register A2A gRPC handler for deployment '%s'",
                        deploymentUnit.getName());
            }
        }

        @Override
        public void stop(StopContext context) {
            // The gRPC subsystem handles cleanup via removeDeploymentServices
        }
    }
}
