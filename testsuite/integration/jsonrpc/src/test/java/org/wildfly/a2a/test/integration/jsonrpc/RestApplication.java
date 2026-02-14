/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.jsonrpc;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application for test endpoints.
 * <p>
 * JAX-RS resources are discovered automatically via CDI bean scanning.
 * The A2A subsystem's deployment processor registers the A2A JAX-RS resources
 * and providers from modules automatically.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
}
