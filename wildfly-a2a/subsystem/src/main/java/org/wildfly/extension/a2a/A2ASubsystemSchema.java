/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

import org.jboss.as.controller.SubsystemSchema;
import org.jboss.as.controller.persistence.xml.ResourceXMLParticleFactory;
import org.jboss.as.controller.persistence.xml.SubsystemResourceRegistrationXMLElement;
import org.jboss.as.controller.persistence.xml.SubsystemResourceXMLSchema;
import org.jboss.as.controller.xml.VersionedNamespace;
import org.jboss.staxmapper.IntVersion;

/**
 * Enumeration of A2A subsystem schema versions.
 */
public enum A2ASubsystemSchema implements SubsystemResourceXMLSchema<A2ASubsystemSchema> {
    VERSION_1_0(1, 0),
    ;

    static final A2ASubsystemSchema CURRENT = VERSION_1_0;

    private final ResourceXMLParticleFactory factory = ResourceXMLParticleFactory.newInstance(this);
    private final VersionedNamespace<IntVersion, A2ASubsystemSchema> namespace;

    A2ASubsystemSchema(int major, int minor) {
        this.namespace = SubsystemSchema.createLegacySubsystemURN(A2ASubsystemRegistrar.NAME, new IntVersion(major, minor));
    }

    @Override
    public VersionedNamespace<IntVersion, A2ASubsystemSchema> getNamespace() {
        return this.namespace;
    }

    @Override
    public SubsystemResourceRegistrationXMLElement getSubsystemXMLElement() {
        return this.factory.subsystemElement(A2ASubsystemRegistrar.REGISTRATION).build();
    }
}
