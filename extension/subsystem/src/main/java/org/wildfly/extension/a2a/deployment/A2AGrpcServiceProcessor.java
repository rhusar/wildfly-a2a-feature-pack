/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import java.util.List;
import java.util.function.Supplier;

import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.modules.Module;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.a2a.A2ALogger;
import org.wildfly.extension.grpc.WildFlyGrpcDeploymentRegistry;

/**
 * Deployment processor that manually registers the A2A gRPC handler with the WildFly gRPC deployment registry.
 *
 * This bypasses the Jandex annotation scanning approach which has issues with module class visibility.
 * Instead, we directly register the WildFlyGrpcHandler class with the gRPC subsystem's deployment registry.
 *
 * @author Radoslav Husar
 */
public class A2AGrpcServiceProcessor implements DeploymentUnitProcessor {

    private static final String GRPC_SERVER_CAPABILITY = "org.wildfly.grpc.server";

    /**
     * The A2A gRPC handlers that may be present in an installation. Each is registered with the gRPC subsystem only when its
     * module and classes are available, so a deployment may expose the v1.0 handler, the v0.3 handler, or both.
     */
    private enum GrpcHandler {
        V1_0("a2a-grpc-registration",
                "org.wildfly.a2a.jakarta.grpc",
                "org.wildfly.a2a.jakarta.grpc.WildFlyGrpcHandler",
                "org.wildfly.a2a.jakarta.grpc.A2AExtensionsInterceptor"),
        V0_3("a2a-grpc-registration-0.3",
                "org.wildfly.a2a.jakarta.compat03.grpc",
                "org.wildfly.a2a.jakarta.grpc.compat03.WildFlyGrpcHandler_v0_3",
                "org.wildfly.a2a.jakarta.grpc.compat03.A2AExtensionsInterceptor_v0_3");

        final String serviceSuffix;
        final String module;
        final String handlerClass;
        final String interceptorClass;

        GrpcHandler(String serviceSuffix, String module, String handlerClass, String interceptorClass) {
            this.serviceSuffix = serviceSuffix;
            this.module = module;
            this.handlerClass = handlerClass;
            this.interceptorClass = interceptorClass;
        }
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        // TODO needs i18n in this class
        A2ALogger.ROOT_LOGGER.debugf("A2AGrpcServiceProcessor running for deployment '%s'", deploymentUnit.getName());

        // Check if gRPC capability is available
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        if (support == null || !support.hasCapability(GRPC_SERVER_CAPABILITY)) {
            A2ALogger.ROOT_LOGGER.infof("gRPC capability not available, skipping A2A gRPC service registration for '%s'", deploymentUnit.getName());
            return;
        }

        for (GrpcHandler handler : GrpcHandler.values()) {
            registerHandler(phaseContext, deploymentUnit, support, handler);
        }
    }

    private void registerHandler(DeploymentPhaseContext phaseContext, DeploymentUnit deploymentUnit,
                                 CapabilityServiceSupport support, GrpcHandler handler) {
        // Load the handler and interceptor from their module to avoid a compile-time dependency.
        // A handler whose module/classes are absent from the deployment is simply skipped.
        Class<? extends BindableService> handlerClass;
        List<ServerInterceptor> interceptors;
        try {
            Module module = Module.getBootModuleLoader().loadModule(handler.module);
            @SuppressWarnings("unchecked")
            Class<? extends BindableService> clazz = (Class<? extends BindableService>) module.getClassLoader().loadClass(handler.handlerClass);
            handlerClass = clazz;
            @SuppressWarnings("unchecked")
            Class<? extends ServerInterceptor> interceptorClass = (Class<? extends ServerInterceptor>) module.getClassLoader().loadClass(handler.interceptorClass);
            interceptors = List.of(interceptorClass.getConstructor().newInstance());
        } catch (Exception e) {
            A2ALogger.ROOT_LOGGER.debugf("A2A gRPC handler '%s' not available for '%s', skipping: %s", handler.handlerClass, deploymentUnit.getName(), e.getMessage());
            return;
        }

        // Install a service that depends on the gRPC registry and registers this handler
        ServiceName serviceName = support.getCapabilityServiceName(GRPC_SERVER_CAPABILITY);
        ServiceName a2aGrpcServiceName = deploymentUnit.getServiceName().append(handler.serviceSuffix);

        ServiceBuilder<?> builder = phaseContext.getRequirementServiceTarget().addService();
        builder.provides(a2aGrpcServiceName);
        Supplier<WildFlyGrpcDeploymentRegistry> registrySupplier = builder.requires(serviceName);

        builder.setInstance(new A2AGrpcRegistrationService(deploymentUnit, handlerClass, interceptors, registrySupplier));
        builder.install();

        A2ALogger.ROOT_LOGGER.infof("Installed A2A gRPC registration service (%s) for deployment '%s'",
                handler.handlerClass, deploymentUnit.getName());
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        // The gRPC subsystem handles cleanup via removeDeploymentServices
    }

    /**
     * A small service that registers the A2A gRPC handler when started, allowing us to properly depend on the gRPC registry service.
     */
    private record A2AGrpcRegistrationService(
            DeploymentUnit deploymentUnit,
            Class<? extends BindableService> handlerClass,
            List<ServerInterceptor> interceptors,
            Supplier<WildFlyGrpcDeploymentRegistry> registrySupplier
    ) implements Service {

        @Override
        public void start(StartContext context) {
            WildFlyGrpcDeploymentRegistry registry = registrySupplier.get();
            if (registry == null) {
                A2ALogger.ROOT_LOGGER.warnf("gRPC deployment registry is not available, skipping A2A gRPC service registration for '%s'", deploymentUnit.getName());
                return;
            }

            A2ALogger.ROOT_LOGGER.debugf("Got gRPC registry: %s", registry.getClass().getName());

            try {
                registry.addService(deploymentUnit, handlerClass, interceptors);
                A2ALogger.ROOT_LOGGER.infof("Registered A2A gRPC handler '%s' for deployment '%s'", handlerClass.getName(), deploymentUnit.getName());
            } catch (Exception e) {
                A2ALogger.ROOT_LOGGER.warnf(e, "Failed to register A2A gRPC handler for deployment '%s'", deploymentUnit.getName());
            }
        }

        @Override
        public void stop(StopContext context) {
            // The gRPC subsystem handles cleanup via removeDeploymentServices
        }
    }
}
