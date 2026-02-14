/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import org.jboss.as.jaxrs.deployment.JaxrsAttachments;
import org.jboss.as.jaxrs.deployment.ResteasyDeploymentData;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.modules.Module;
import org.wildfly.extension.a2a.A2ALogger;

/**
 * Deployment processor that registers A2A JAX-RS resources and providers with RESTEasy.
 *
 * This processor runs at POST_MODULE phase (after JAX-RS annotation scanning) to add A2A classes from modules to the RESTEasy deployment data.
 * RESTEasy doesn't scan module dependencies for @Path and @Provider annotations, so we need to register them explicitly.
 *
 * Classes are registered conditionally based on availability to support deployments that only use specific transports.
 */
public class A2AJaxrsProcessor implements DeploymentUnitProcessor {

    /**
     * JAX-RS resource classes to register with RESTEasy.
     */
    private static final String[] JAXRS_RESOURCE_CLASSES = {
            "org.wildfly.extras.a2a.server.apps.jsonrpc.A2AServerResource",
            "org.wildfly.extras.a2a.server.apps.rest.A2ARestServerResource"
    };

    /**
     * JAX-RS provider classes to register with RESTEasy.
     */
    private static final String[] JAXRS_PROVIDER_CLASSES = {
            "org.wildfly.extras.a2a.server.apps.jsonrpc.A2ARequestFilter"
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        // Only process WAR deployments that have JAX-RS deployment data
        ResteasyDeploymentData resteasyData = deploymentUnit.getAttachment(JaxrsAttachments.RESTEASY_DEPLOYMENT_DATA);
        if (resteasyData == null) {
            A2ALogger.ROOT_LOGGER.debugf("No JAX-RS deployment data for %s, skipping A2A JAX-RS registration", deploymentUnit.getName());
            return;
        }

        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ClassLoader classLoader = module != null ? module.getClassLoader() : getClass().getClassLoader();

        A2ALogger.ROOT_LOGGER.debugf("Registering A2A JAX-RS classes for deployment '%s'", deploymentUnit.getName());

        // Add A2A JAX-RS resource classes if available
        for (String resourceClass : JAXRS_RESOURCE_CLASSES) {
            if (isClassAvailable(resourceClass, classLoader)) {
                A2ALogger.ROOT_LOGGER.debugf("Registering JAX-RS resource: %s", resourceClass);
                resteasyData.getScannedResourceClasses().add(resourceClass);
            }
        }

        // Add A2A JAX-RS provider classes if available
        for (String providerClass : JAXRS_PROVIDER_CLASSES) {
            if (isClassAvailable(providerClass, classLoader)) {
                A2ALogger.ROOT_LOGGER.debugf("Registering JAX-RS provider: %s", providerClass);
                resteasyData.getScannedProviderClasses().add(providerClass);
            }
        }
    }

    private boolean isClassAvailable(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
