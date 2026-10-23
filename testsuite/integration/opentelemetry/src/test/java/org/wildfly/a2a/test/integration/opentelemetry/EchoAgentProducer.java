/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.opentelemetry;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.PublicAgentCard;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;
import org.a2aproject.sdk.spec.AgentSkill;

/**
 * The smallest agent a request can be sent to: an {@link AgentExecutor} that echoes the user input
 * back, plus the public agent card advertising it over JSON-RPC.
 *
 * <p>The point of the deployment is the span the {@code a2a-server-opentelemetry} decorator wraps
 * around the request, so the agent itself does as little as possible.
 */
@ApplicationScoped
public class EchoAgentProducer {

    static final String AGENT_URL = "http://localhost:8080";

    @Produces
    @PublicAgentCard
    public AgentCard agentCard() {
        return AgentCard.builder()
                .name("OpenTelemetry Test Agent")
                .description("Echoes the user input back.")
                .version("1.0.0")
                .supportedInterfaces(List.of(new AgentInterface("JSONRPC", AGENT_URL)))
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(AgentSkill.builder()
                        .id("echo")
                        .name("Echo")
                        .description("Echoes the user input back")
                        .tags(List.of("test"))
                        .examples(List.of())
                        .build()))
                .build();
    }

    @Produces
    public AgentExecutor agentExecutor() {
        return new AgentExecutor() {
            @Override
            public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                emitter.sendMessage("echo: " + context.getUserInput());
            }
        };
    }
}
