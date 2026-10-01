/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * Deployment unit processors contributed to the deployment chain by the {@code a2a} subsystem.
 *
 * <p>Together they make an ordinary deployment into an A2A agent without requiring the application to bundle the
 * A2A SDK or declare module dependencies of its own:
 *
 * <ul>
 * <li>{@link org.wildfly.extension.a2a.deployment.A2ADependencyProcessor} adds the A2A SDK and transport modules
 * as dependencies of the deployment.</li>
 * <li>{@link org.wildfly.extension.a2a.deployment.A2AJaxrsProcessor} registers the JSON-RPC and REST transport
 * resources and providers with RESTEasy, which does not scan module dependencies for JAX-RS annotations.</li>
 * <li>{@link org.wildfly.extension.a2a.deployment.A2AGrpcServiceProcessor} registers the A2A gRPC handlers with
 * the WildFly gRPC subsystem's deployment registry.</li>
 * </ul>
 *
 * <p>Each processor registers only the classes whose modules are actually present, so a server provisioned with a
 * subset of the transport layers gets a matching subset of the A2A endpoints.
 *
 * @author Radoslav Husar
 */
package org.wildfly.extension.a2a.deployment;
