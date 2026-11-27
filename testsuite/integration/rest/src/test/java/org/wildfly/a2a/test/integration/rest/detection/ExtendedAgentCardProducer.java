/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.detection;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.a2aproject.sdk.server.ExtendedAgentCard;
import org.a2aproject.sdk.spec.AgentCapabilities;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.AgentInterface;

/**
 * Produces only an {@code @ExtendedAgentCard}, which the SDK falls back to when there is no {@code @PublicAgentCard}.
 */
@ApplicationScoped
public class ExtendedAgentCardProducer {

    @Produces
    @ExtendedAgentCard
    public AgentCard extendedAgentCard() {
        return AgentCard.builder()
                .name("Extended Agent Card Only")
                .description("Agent providing only an extended agent card")
                .version("1.0.0")
                .supportedInterfaces(List.of(new AgentInterface("HTTP+JSON", "http://localhost:8080/extended")))
                .capabilities(AgentCapabilities.builder().build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of())
                .build();
    }
}
