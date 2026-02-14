/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.grpc;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application for test endpoints.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
}
