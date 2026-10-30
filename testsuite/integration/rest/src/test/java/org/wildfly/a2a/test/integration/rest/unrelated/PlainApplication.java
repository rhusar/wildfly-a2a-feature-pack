/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.unrelated;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application of the deployment that does not use A2A.
 */
@ApplicationPath("/")
public class PlainApplication extends Application {
}
