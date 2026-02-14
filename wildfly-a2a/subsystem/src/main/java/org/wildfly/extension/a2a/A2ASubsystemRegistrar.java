/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.SubsystemResourceRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.deployment.Phase;
import org.wildfly.extension.a2a.deployment.A2ADependencyProcessor;
import org.wildfly.extension.a2a.deployment.A2AGrpcServiceProcessor;
import org.wildfly.extension.a2a.deployment.A2AJaxrsProcessor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.SubsystemResourceDefinitionRegistrar;

/**
 * Registrar for the A2A subsystem.
 */
class A2ASubsystemRegistrar implements SubsystemResourceDefinitionRegistrar {

    static final String NAME = "a2a";
    static final SubsystemResourceRegistration REGISTRATION = SubsystemResourceRegistration.of(NAME);
    static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(NAME, A2ASubsystemRegistrar.class);

    @Override
    public ManagementResourceRegistration register(SubsystemRegistration parent, ManagementResourceRegistrationContext context) {
        A2ALogger.ROOT_LOGGER.subsystemActivated();

        parent.setHostCapable();

        ManagementResourceRegistration registration = parent.registerSubsystemModel(ResourceDefinition.builder(REGISTRATION, RESOLVER).build());

        ResourceDescriptor descriptor = ResourceDescriptor.builder(RESOLVER)
                .withDeploymentChainContributor(target -> {
                    target.addDeploymentProcessor(NAME, Phase.DEPENDENCIES, A2APhases.PHASE_DEPENDENCIES_A2A, new A2ADependencyProcessor());
                    target.addDeploymentProcessor(NAME, Phase.INSTALL, A2APhases.PHASE_INSTALL_A2A_GRPC, new A2AGrpcServiceProcessor());
                    target.addDeploymentProcessor(NAME, Phase.POST_MODULE, A2APhases.PHASE_POST_MODULE_A2A_JAXRS, new A2AJaxrsProcessor());
                })
                .build();
        ManagementResourceRegistrar.of(descriptor).register(registration);

        return registration;
    }
}
