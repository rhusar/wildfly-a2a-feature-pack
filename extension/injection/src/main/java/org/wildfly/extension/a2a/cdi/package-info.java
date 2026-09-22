/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * CDI portable extension that makes the A2A beans shipped as JBoss modules visible to deployments.
 *
 * <p>The A2A transport classes live in modules rather than in {@code WEB-INF/lib}, so their jars are not bean
 * archives and Weld does not discover them. {@link org.wildfly.extension.a2a.cdi.A2ACdiExtension} adds those
 * classes as beans explicitly, so an application can inject them as if they had been deployed with it.
 *
 * @author Radoslav Husar
 */
package org.wildfly.extension.a2a.cdi;
