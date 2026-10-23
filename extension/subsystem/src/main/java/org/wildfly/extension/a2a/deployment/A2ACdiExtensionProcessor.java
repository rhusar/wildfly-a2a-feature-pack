/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.wildfly.extension.a2a.cdi.A2ACdiExtension;

/**
 * Deployment processor that registers the {@link A2ACdiExtension} with A2A server deployments.
 *
 * The extension is registered per deployment rather than discovered through {@link java.util.ServiceLoader}, so that it
 * only runs in deployments marked by {@link A2ADeploymentMarker} as A2A server deployments.
 */
public class A2ACdiExtensionProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (!A2ADeploymentMarker.isMarkedServer(deploymentUnit)) {
            return;
        }

        // A2A server deployments are always Weld deployments, see A2ADependencyProcessor
        CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
        support.getOptionalCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class)
                .ifPresent(weldCapability -> weldCapability.registerExtensionInstance(new A2ACdiExtension(), deploymentUnit));
    }
}
