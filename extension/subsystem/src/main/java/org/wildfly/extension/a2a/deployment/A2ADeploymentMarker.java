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
 * Detects A2A server and A2A client deployments, and marks a deployment unit as an A2A server deployment, so that A2A
 * deployment processors leave other deployments alone.
 *
 * The mark is set on the deployment unit itself and never propagated to its parent, so an A2A WAR inside an EAR does not
 * turn its sibling subdeployments into A2A deployments. The detection reads the deployment's composite annotation index
 * though, which for a subdeployment can also cover the library jars of its EAR, so A2A usage in an {@code ear/lib} jar is
 * detected in every subdeployment that sees that jar.
 *
 * Class references are read from the Jandex index, so a deployment jar shipping a pre-built {@code META-INF/jandex.idx}
 * created by a Jandex version that does not record class references is not recognised through them.
 */
public final class A2ADeploymentMarker {

    private static final AttachmentKey<Boolean> SERVER_ATTACHMENT_KEY = AttachmentKey.create(Boolean.class);

    /**
     * Qualifiers of the agent card an A2A server deployment provides - the SDK serves the {@code @PublicAgentCard} and falls
     * back to the {@code @ExtendedAgentCard} when there is no public one.
     */
    private static final List<DotName> AGENT_CARD_QUALIFIERS = List.of(
            DotName.createSimple("org.a2aproject.sdk.server.PublicAgentCard"),
            DotName.createSimple("org.a2aproject.sdk.server.ExtendedAgentCard")
    );

    /**
     * Every A2A server deployment provides an agent executor. It is matched by the classes referencing it rather than by
     * the classes implementing it, so that an executor produced as a lambda or qualified per tenant is found as well.
     */
    private static final DotName AGENT_EXECUTOR = DotName.createSimple("org.a2aproject.sdk.server.agentexecution.AgentExecutor");

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

    /**
     * @return whether the deployment unit was marked as an A2A server deployment
     */
    public static boolean isMarkedServer(DeploymentUnit deploymentUnit) {
        Boolean marked = deploymentUnit.getAttachment(SERVER_ATTACHMENT_KEY);
        return (marked != null) && marked;
    }

    static void clearMark(DeploymentUnit deploymentUnit) {
        deploymentUnit.removeAttachment(SERVER_ATTACHMENT_KEY);
    }

    static boolean usesA2AServer(DeploymentUnit deploymentUnit) {
        CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex != null) {
            for (DotName qualifier : AGENT_CARD_QUALIFIERS) {
                if (!compositeIndex.getAnnotations(qualifier).isEmpty()) {
                    A2ALogger.ROOT_LOGGER.debugf("Deployment '%s' is an A2A server deployment - @%s annotation found.", deploymentUnit.getName(), qualifier);
                    return true;
                }
            }
            // CompositeIndex does not expose the class users, so query each underlying index
            for (Index index : compositeIndex.getIndexes()) {
                if (!index.getKnownUsers(AGENT_EXECUTOR).isEmpty()) {
                    A2ALogger.ROOT_LOGGER.debugf("Deployment '%s' is an A2A server deployment - %s is used.", deploymentUnit.getName(), AGENT_EXECUTOR);
                    return true;
                }
            }
        }

        A2ALogger.ROOT_LOGGER.debugf("No A2A agent card or agent executor found in deployment '%s'.", deploymentUnit.getName());

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
