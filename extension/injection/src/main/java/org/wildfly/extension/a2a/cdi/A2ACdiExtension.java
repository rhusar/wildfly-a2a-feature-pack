/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterTypeDiscovery;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;

/**
 * CDI extension that registers A2A beans from JBoss modules.
 *
 * <p>Since the A2A transport classes live in JBoss modules (not {@code WEB-INF/lib}) whose jars do
 * not carry a {@code META-INF/beans.xml}, CDI does not automatically discover them. This extension
 * programmatically adds them as CDI beans during the type discovery phase.
 *
 * <p>Classes are registered conditionally based on availability to support deployments that only
 * use specific transports.
 */
public class A2ACdiExtension implements Extension {

    /**
     * The v0.3 SDK request->response converter is an {@code @ApplicationScoped} bean with only an
     * {@code @Inject} constructor (no no-args constructor), which is not proxyable. When it is loaded
     * from a JBoss module (rather than bundled in the application archive) Weld rejects it. Since it
     * is a stateless adapter over the v1.0 {@code RequestHandler}, relax its scope to
     * {@code @Dependent} so no client proxy is required.
     */
    <T> void relaxV03ConverterScope(@Observes @WithAnnotations(ApplicationScoped.class) ProcessAnnotatedType<T> pat) {
        if ("org.a2aproject.sdk.compat03.conversion.Convert_v0_3_To10RequestHandler"
                .equals(pat.getAnnotatedType().getJavaClass().getName())) {
            pat.configureAnnotatedType()
                    .remove(a -> a.annotationType().equals(ApplicationScoped.class))
                    .add(Dependent.Literal.INSTANCE);
        }
    }

    void afterTypeDiscovery(@Observes AfterTypeDiscovery event, BeanManager beanManager) {
        ClassLoader classLoader = getClass().getClassLoader();

        // Shared HTTP routing filters. They dispatch an incoming request to the versioned resource path of the
        // A2AVersionProvider selected by the A2A-Version header, so they are required by both HTTP transports.
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.common.AgentCardRoutingFilter", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.common.A2AJsonRpcAcceptFilter", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.common.A2ARestVersionRoutingFilter", classLoader);

        // JSON-RPC transport
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.jsonrpc.A2AServerResource", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.jsonrpc.JsonRpcVersionProvider_v1_0", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.jsonrpc.JsonRpcMethodProvider_v1_0", classLoader);

        // REST transport
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.rest.A2ARestServerResource", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.rest.RestVersionProvider_v1_0", classLoader);

        // gRPC transport
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.grpc.GrpcBeanInitializer", classLoader);

        // JSON-RPC transport (v0.3 backward compatibility)
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.jsonrpc.compat03.A2AServerResource_v0_3", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.jsonrpc.compat03.JsonRpcVersionProvider_v0_3", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.jsonrpc.compat03.JsonRpcMethodProvider_v0_3", classLoader);

        // REST transport (v0.3 backward compatibility)
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.rest.compat03.A2ARestServerResource_v0_3", classLoader);
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.rest.compat03.RestVersionProvider_v0_3", classLoader);

        // gRPC transport (v0.3 backward compatibility)
        tryAddAnnotatedType(event, beanManager, "org.wildfly.a2a.jakarta.grpc.compat03.GrpcBeanInitializer_v0_3", classLoader);
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
