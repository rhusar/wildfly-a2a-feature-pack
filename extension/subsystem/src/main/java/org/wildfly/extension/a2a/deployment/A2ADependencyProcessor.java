/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Optional;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.wildfly.extension.a2a.A2ALogger;

/**
 * Deployment processor that adds A2A module dependencies to deployments.
 *
 * A2A server deployments, i.e. CDI deployments that provide a {@code @PublicAgentCard}, get the SDK, client and server
 * modules, with their {@code META-INF} imported for CDI bean discovery. A2A client deployments, i.e. deployments that use
 * the A2A client API, get the SDK and client modules only. Other deployments get no A2A modules.
 * See {@link A2ADeploymentMarker}.
 */
public class A2ADependencyProcessor implements DeploymentUnitProcessor {

    /**
     * A2A SDK modules, always provisioned by the {@code a2a} layer.
     */
    private static final String[] SDK_MODULES = {
            "org.a2aproject.sdk.common",
            "org.a2aproject.sdk.spec",
            "org.a2aproject.sdk.spec-grpc",
            "org.a2aproject.sdk.jsonrpc-common",
            "org.a2aproject.sdk.http-client"
    };

    /**
     * A2A client modules - optional because a server may be provisioned without the client-side layers.
     */
    private static final String[] CLIENT_MODULES = {
            "org.a2aproject.sdk.client",
            "org.a2aproject.sdk.client-transport-spi",
            "org.a2aproject.sdk.client-transport-jsonrpc",
            "org.a2aproject.sdk.client-transport-grpc",
            "org.a2aproject.sdk.client-transport-rest"
    };

    /**
     * Modules added to A2A server deployments only.
     */
    private static final String[] SERVER_MODULES = {
            "org.wildfly.extension.a2a.injection",
            "com.google.code.gson",
            "com.google.protobuf",
            "com.google.guava"
    };

    /**
     * Optional modules added to A2A server deployments only - added based on what's available in the provisioned server,
     * because a server may be provisioned without the server-side layers.
     */
    private static final String[] OPTIONAL_SERVER_MODULES = {
            "org.a2aproject.sdk.server-common",
            "org.a2aproject.sdk.transport.jsonrpc",
            "org.a2aproject.sdk.transport.grpc",
            "org.a2aproject.sdk.transport.rest",
            "io.smallrye.reactive.mutiny.zero",
            "org.a2aproject.sdk.microprofile-config",
            // v0.3 backward-compatibility SDK modules (present only when a v0.3 layer is provisioned).
            // These carry the v0.3 spec types and request handlers (@ApplicationScoped CDI beans).
            "org.a2aproject.sdk.compat03-spec",
            "org.a2aproject.sdk.compat03-spec-grpc",
            "org.a2aproject.sdk.compat03-server-conversion",
            "org.a2aproject.sdk.compat03-transport-jsonrpc",
            "org.a2aproject.sdk.compat03-transport-grpc",
            "org.a2aproject.sdk.compat03-transport-rest"
    };

    /**
     * Jakarta modules containing JAX-RS resources and providers, added to A2A server deployments only.
     * These are added as system dependencies with META-INF filters for CDI discovery.
     */
    private static final String[] JAKARTA_MODULES = {
            "org.wildfly.a2a.jakarta.common",
            "org.wildfly.a2a.jakarta.jsonrpc",
            "org.wildfly.a2a.jakarta.grpc",
            "org.wildfly.a2a.jakarta.rest",
            // v0.3 backward-compatibility transports (added only when present in the deployment)
            "org.wildfly.a2a.jakarta.compat03.jsonrpc",
            "org.wildfly.a2a.jakarta.compat03.grpc",
            "org.wildfly.a2a.jakarta.compat03.rest"
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        A2ALogger.ROOT_LOGGER.debugf("Processing deployment '%s' for A2A", deploymentUnit.getName());
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();

        if (isA2AServerDeployment(deploymentUnit)) {
            A2ADeploymentMarker.markServer(deploymentUnit);
            addServerDependencies(moduleSpecification, moduleLoader);
        }

        if (A2ADeploymentMarker.usesA2AClient(deploymentUnit)) {
            A2ADeploymentMarker.markClient(deploymentUnit);
            // The server dependencies already include the client modules
            if (!A2ADeploymentMarker.isMarkedServer(deploymentUnit)) {
                addClientDependencies(moduleSpecification, moduleLoader);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit deploymentUnit) {
        A2ADeploymentMarker.clearMarks(deploymentUnit);
    }

    private static boolean isA2AServerDeployment(DeploymentUnit deploymentUnit) {
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        Optional<WeldCapability> weldCapability = support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
        return weldCapability.isPresent() && weldCapability.get().isPartOfWeldDeployment(deploymentUnit)
                && A2ADeploymentMarker.hasA2AServerAnnotations(deploymentUnit);
    }

    /**
     * Adds the SDK and client modules, importing services so that the client finds its transports and HTTP client
     * through {@link java.util.ServiceLoader}. META-INF is not imported, so CDI does not discover beans from these modules.
     */
    private static void addClientDependencies(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        for (String module : SDK_MODULES) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, module).setImportServices(true).build());
        }
        for (String module : CLIENT_MODULES) {
            moduleSpecification.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, module).setOptional(true).setImportServices(true).build());
        }
    }

    private static void addServerDependencies(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader) {
        // Add exported modules with META-INF import for CDI bean discovery
        for (String module : SDK_MODULES) {
            addServerDependency(moduleSpecification, moduleLoader, module, false);
        }
        for (String module : SERVER_MODULES) {
            addServerDependency(moduleSpecification, moduleLoader, module, false);
        }

        // Add optional client and transport modules with META-INF import for CDI bean discovery
        for (String module : CLIENT_MODULES) {
            addServerDependency(moduleSpecification, moduleLoader, module, true);
        }
        for (String module : OPTIONAL_SERVER_MODULES) {
            addServerDependency(moduleSpecification, moduleLoader, module, true);
        }

        // Add Jakarta modules as system dependencies with META-INF filters for CDI discovery
        for (String module : JAKARTA_MODULES) {
            ModuleDependency modDep = ModuleDependency.Builder.of(moduleLoader, module)
                    .setOptional(true)
                    .setExport(true)
                    .setImportServices(true)
                    .build();
            // Import META-INF and subdirectories for CDI and services discovery
            modDep.addImportFilter(PathFilters.getMetaInfSubdirectoriesFilter(), true);
            modDep.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moduleSpecification.addSystemDependency(modDep);
        }
    }

    private static void addServerDependency(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader, String module, boolean optional) {
        ModuleDependency modDep = ModuleDependency.Builder.of(moduleLoader, module)
                .setOptional(optional)
                .setExport(true)
                .setImportServices(true)
                .build();
        // Import META-INF to allow CDI to discover beans from modules
        modDep.addImportFilter(PathFilters.getMetaInfFilter(), true);
        moduleSpecification.addSystemDependency(modDep);
    }
}
