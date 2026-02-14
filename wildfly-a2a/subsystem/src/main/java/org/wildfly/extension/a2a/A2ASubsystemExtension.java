/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

import org.wildfly.subsystem.SubsystemConfiguration;
import org.wildfly.subsystem.SubsystemExtension;
import org.wildfly.subsystem.SubsystemPersistence;

/**
 * The extension class for the WildFly A2A extension.
 */
public class A2ASubsystemExtension extends SubsystemExtension<A2ASubsystemSchema> {

    public A2ASubsystemExtension() {
        super(SubsystemConfiguration.of(A2ASubsystemRegistrar.NAME, A2ASubsystemModel.CURRENT, A2ASubsystemRegistrar::new), SubsystemPersistence.of(A2ASubsystemSchema.CURRENT));
    }
}
