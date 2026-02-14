/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.cdi;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

/**
 * CDI extension that registers A2A beans from JBoss modules.
 *
 * Since A2A classes are in JBoss modules (not WEB-INF/lib), CDI doesn't automatically discover them.
 * This extension programmatically adds them as CDI beans during the type discovery phase.
 *
 * Classes are registered conditionally based on availability to support deployments that only use specific transports.
 */
public class A2ACdiExtension implements Extension {

    void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
        ClassLoader classLoader = getClass().getClassLoader();

        // JSON-RPC transport
        tryAddAnnotatedType(event, beanManager, "org.wildfly.extras.a2a.server.apps.jsonrpc.A2AServerResource", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.extras.a2a.server.apps.jsonrpc.A2ARequestFilter", classLoader);

        // REST transport
        tryAddAnnotatedType(event, beanManager, "org.wildfly.extras.a2a.server.apps.rest.A2ARestServerResource", classLoader);

        // gRPC transport
        tryAddAnnotatedType(event, beanManager, "org.wildfly.extras.a2a.server.apps.grpc.GrpcBeanInitializer", classLoader);
    }

    private void tryAddAnnotatedType(AfterTypeDiscovery event, BeanManager beanManager, String className, ClassLoader classLoader) {
        try {
            Class<?> clazz = classLoader.loadClass(className);
            AnnotatedType<?> annotatedType = beanManager.createAnnotatedType(clazz);
            event.addAnnotatedType(annotatedType, className);
        } catch (Throwable e) {
            // Class not available in this deployment, skip registration
        }
    }
}
