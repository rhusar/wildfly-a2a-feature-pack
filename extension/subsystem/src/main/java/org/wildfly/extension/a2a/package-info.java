/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * The WildFly {@code a2a} subsystem: the extension, its management model and its XML schema.
 *
 * <p>{@link org.wildfly.extension.a2a.A2ASubsystemExtension} is the entry point registered with the WildFly
 * management layer. It installs the {@code a2a} subsystem resource, which contributes the deployment unit
 * processors in {@link org.wildfly.extension.a2a.deployment} to the deployment chain so that A2A agents
 * deployed to the server are wired to the A2A transports provisioned by this feature-pack.
 *
 * <p>The subsystem is {@link org.jboss.as.version.Stability#EXPERIMENTAL experimental}, so it is only
 * available on servers running at the experimental stability level.
 *
 * @author Radoslav Husar
 */
package org.wildfly.extension.a2a;
