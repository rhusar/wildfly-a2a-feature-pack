/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.opentelemetry;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS application for the A2A JSON-RPC resources the subsystem adds to the deployment.
 */
@ApplicationPath("/")
public class RestApplication extends Application {
}
