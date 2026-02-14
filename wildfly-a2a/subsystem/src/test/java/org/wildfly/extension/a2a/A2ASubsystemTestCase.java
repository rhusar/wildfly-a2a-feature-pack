/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

import java.util.EnumSet;

import org.jboss.as.subsystem.test.AbstractSubsystemSchemaTest;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test case for A2A subsystem XML parsing and initialization.
 */
@RunWith(Parameterized.class)
public class A2ASubsystemTestCase extends AbstractSubsystemSchemaTest<A2ASubsystemSchema> {

    @Parameters
    public static Iterable<A2ASubsystemSchema> parameters() {
        return EnumSet.allOf(A2ASubsystemSchema.class);
    }

    public A2ASubsystemTestCase(A2ASubsystemSchema schema) {
        super(A2ASubsystemRegistrar.NAME, new A2ASubsystemExtension(), schema, A2ASubsystemSchema.CURRENT);
    }
}
