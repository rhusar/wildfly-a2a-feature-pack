/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

import org.jboss.as.controller.SubsystemModel;
import org.jboss.as.controller.ModelVersion;

/**
 * Enumeration of A2A subsystem model versions.
 */
enum A2ASubsystemModel implements SubsystemModel {
    VERSION_1_0_0(1, 0, 0),
    ;

    static final A2ASubsystemModel CURRENT = VERSION_1_0_0;

    private final ModelVersion version;

    A2ASubsystemModel(int major, int minor, int micro) {
        this.version = ModelVersion.create(major, minor, micro);
    }

    @Override
    public ModelVersion getVersion() {
        return this.version;
    }
}
