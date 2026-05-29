/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.filter.PathFilters;
import org.wildfly.extension.a2a.A2ALogger;

/**
 * Deployment processor that adds A2A module dependencies to deployments.
 */
public class A2ADependencyProcessor implements DeploymentUnitProcessor {

    /**
     * Modules exported to deployments.
     */
    private static final String[] EXPORTED_MODULES = {
            "org.wildfly.extension.a2a.injection",
            "org.a2aproject.sdk.common",
            "org.a2aproject.sdk.spec",
            "org.a2aproject.sdk.spec-grpc",
            "org.a2aproject.sdk.jsonrpc-common",
            "org.a2aproject.sdk.http-client",
            "org.a2aproject.sdk.client",
            "org.a2aproject.sdk.client-transport-spi",
            "org.a2aproject.sdk.client-transport-jsonrpc",
            "org.a2aproject.sdk.client-transport-grpc",
            "org.a2aproject.sdk.client-transport-rest",
            "org.a2aproject.sdk.server-common",
            "com.google.code.gson",
            "com.google.protobuf",
            "com.google.guava"
    };

    /**
     * Optional transport modules - added based on what's available.
     */
    private static final String[] OPTIONAL_MODULES = {
            "org.a2aproject.sdk.transport.jsonrpc",
            "org.a2aproject.sdk.transport.grpc",
            "org.a2aproject.sdk.transport.rest",
            "io.smallrye.reactive.mutiny.zero",
            "org.a2aproject.sdk.microprofile-config"
    };

    /**
     * Jakarta modules containing JAX-RS resources and providers.
     * These are added as system dependencies with META-INF filters for CDI discovery.
     */
    private static final String[] JAKARTA_MODULES = {
            "org.wildfly.a2a.jakarta.jsonrpc",
            "org.wildfly.a2a.jakarta.grpc",
            "org.wildfly.a2a.jakarta.rest"
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        A2ALogger.ROOT_LOGGER.debugf("Processing deployment '%s' for A2A", deploymentUnit.getName());
        ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        ModuleLoader moduleLoader = Module.getBootModuleLoader();

        // Add exported modules with META-INF import for CDI bean discovery
        for (String module : EXPORTED_MODULES) {
            ModuleDependency modDep = ModuleDependency.Builder.of(moduleLoader, module)
                    .setExport(true)
                    .setImportServices(true)
                    .build();
            // Import META-INF to allow CDI to discover beans from modules
            modDep.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moduleSpecification.addSystemDependency(modDep);
        }

        // Add optional transport modules with META-INF import for CDI bean discovery
        for (String module : OPTIONAL_MODULES) {
            ModuleDependency modDep = ModuleDependency.Builder.of(moduleLoader, module)
                    .setOptional(true)
                    .setExport(true)
                    .setImportServices(true)
                    .build();
            // Import META-INF to allow CDI to discover beans from modules
            modDep.addImportFilter(PathFilters.getMetaInfFilter(), true);
            moduleSpecification.addSystemDependency(modDep);
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
}
