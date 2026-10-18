/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import java.util.List;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.wildfly.extension.a2a.A2ALogger;

/**
 * Marks a deployment unit as an A2A server deployment and/or an A2A client deployment, so that A2A deployment processors
 * leave other deployments alone.
 *
 * The marks are set on the deployment unit itself and never propagated to its parent, so an A2A WAR inside an EAR does not
 * turn its sibling subdeployments into A2A deployments.
 */
public final class A2ADeploymentMarker {

    private static final AttachmentKey<Boolean> SERVER_ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);
    private static final AttachmentKey<Boolean> CLIENT_ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    /**
     * An A2A server deployment has to provide the public agent card, i.e. an {@code AgentCard} bean qualified with this
     * annotation.
     */
    private static final DotName PUBLIC_AGENT_CARD = DotName.createSimple("org.a2aproject.sdk.server.PublicAgentCard");

    /**
     * Entry points of the A2A client API - a client is created through {@code Client.builder(...)}, and {@code A2A}
     * provides the agent card and message helpers. A class that uses the client references one of these in its constant
     * pool, which is also what WildFly Glow matches the client layer rules against.
     */
    private static final List<DotName> CLIENT_CLASSES = List.of(
            DotName.createSimple("org.a2aproject.sdk.client.Client"),
            DotName.createSimple("org.a2aproject.sdk.client.ClientBuilder"),
            DotName.createSimple("org.a2aproject.sdk.A2A")
    );

    static void markServer(DeploymentUnit deploymentUnit) {
        deploymentUnit.putAttachment(SERVER_ATTACHMENT_KEY, true);
    }

    static void markClient(DeploymentUnit deploymentUnit) {
        deploymentUnit.putAttachment(CLIENT_ATTACHMENT_KEY, true);
    }

    /**
     * @return whether the deployment unit was marked as an A2A server deployment
     */
    public static boolean isMarkedServer(DeploymentUnit deploymentUnit) {
        Boolean marked = deploymentUnit.getAttachment(SERVER_ATTACHMENT_KEY);
        return (marked != null) && marked;
    }

    /**
     * @return whether the deployment unit was marked as an A2A client deployment
     */
    public static boolean isMarkedClient(DeploymentUnit deploymentUnit) {
        Boolean marked = deploymentUnit.getAttachment(CLIENT_ATTACHMENT_KEY);
        return (marked != null) && marked;
    }

    static void clearMarks(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(SERVER_ATTACHMENT_KEY);
        deploymentUnit.removeAttachment(CLIENT_ATTACHMENT_KEY);
    }

    static boolean hasA2AServerAnnotations(DeploymentUnit deploymentUnit) {
        CompositeIndex index = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (index != null && !index.getAnnotations(PUBLIC_AGENT_CARD).isEmpty()) {
            A2ALogger.ROOT_LOGGER.debugf("Deployment '%s' is an A2A server deployment - @%s annotation found.", deploymentUnit.getName(), PUBLIC_AGENT_CARD);
            return true;
        }

        A2ALogger.ROOT_LOGGER.debugf("No @%s annotation found in deployment '%s'.", PUBLIC_AGENT_CARD, deploymentUnit.getName());

        return false;
    }

    static boolean usesA2AClient(DeploymentUnit deploymentUnit) {
        CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex != null) {
            // CompositeIndex does not expose the class users, so query each underlying index
            for (Index index : compositeIndex.getIndexes()) {
                for (DotName clientClass : CLIENT_CLASSES) {
                    if (!index.getKnownUsers(clientClass).isEmpty()) {
                        A2ALogger.ROOT_LOGGER.debugf("Deployment '%s' is an A2A client deployment - %s is used.", deploymentUnit.getName(), clientClass);
                        return true;
                    }
                }
            }
        }

        A2ALogger.ROOT_LOGGER.debugf("No use of the A2A client API found in deployment '%s'.", deploymentUnit.getName());

        return false;
    }

    private A2ADeploymentMarker() {
        // Utility class.
    }
}
