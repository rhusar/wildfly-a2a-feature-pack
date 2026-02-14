/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger for the A2A subsystem.
 */
@MessageLogger(projectCode = "WFA2A", length = 5)
public interface A2ALogger extends BasicLogger {

    A2ALogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), A2ALogger.class, "org.wildfly.extension.a2a");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "A2A subsystem activated")
    void subsystemActivated();
}
