/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

/**
 * Deployment phase priorities for the A2A subsystem.
 */
final class A2APhases {

    /**
     * Deployment processor priority for adding A2A dependencies - after core dependencies.
     */
    static final int PHASE_DEPENDENCIES_A2A = 0x1941;

    /**
     * Deployment processor priority for registering A2A gRPC service with the gRPC subsystem.
     * Runs during INSTALL phase to install a service that registers the WildFlyGrpcHandler with the gRPC deployment registry, bypassing Jandex annotation scanning.
     */
    static final int PHASE_INSTALL_A2A_GRPC = 0x2000;

    /**
     * Deployment processor priority for JAX-RS registration - after JAX-RS annotation scanning.
     * The JAX-RS scanning processor runs at 0x1A00 (POST_MODULE_JAXRS_SCANNING), so we run after that but before the component deployer at 0x1B00.
     */
    static final int PHASE_POST_MODULE_A2A_JAXRS = 0x1A50;

    private A2APhases() {
    }
}
