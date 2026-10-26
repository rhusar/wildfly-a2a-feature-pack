/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.detection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;

/**
 * Produces the {@link AgentExecutor} as a lambda, so the deployment contains no class implementing it.
 */
@ApplicationScoped
public class LambdaAgentExecutorProducer {

    @Produces
    public AgentExecutor agentExecutor() {
        return (context, emitter) -> emitter.complete();
    }
}
