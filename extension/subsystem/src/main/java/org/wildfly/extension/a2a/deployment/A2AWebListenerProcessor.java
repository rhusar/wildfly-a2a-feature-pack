/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.a2a.deployment;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.wildfly.extension.a2a.A2ALogger;

/**
 * Deployment processor that registers the A2A servlet context listeners with web deployments.
 *
 * WildFly builds web metadata from {@code @WebListener} by scanning the deployment's own classes, and the A2A listeners
 * live in JBoss modules, so they are never discovered. This processor adds them to the merged web metadata by hand, the
 * same way {@link A2AJaxrsProcessor} registers module classes RESTEasy does not scan for.
 *
 * It runs between {@code Phase.PARSE_WEB_MERGE_METADATA}, which produces the merged metadata, and
 * {@code Phase.PARSE_WEB_COMPONENTS}, which turns every listener class in it into an EE component description - so the
 * listeners are registered in time to be given {@code @Inject} support rather than being instantiated bare.
 */
public class A2AWebListenerProcessor implements DeploymentUnitProcessor {

    /**
     * The module the listeners are loaded from. Absent from client-only layer combinations.
     */
    private static final String JAKARTA_COMMON_MODULE = "org.wildfly.a2a.jakarta.common";

    /**
     * Servlet context listener classes to register, all in {@link #JAKARTA_COMMON_MODULE}.
     *
     * {@code SSESubscriberLifecycleListener} shuts the {@code sse-disconnect-detector} thread of the
     * {@code @ApplicationScoped SSEHeartbeatScheduler} down from {@code contextDestroyed()}. Without it that thread
     * outlives the deployment, leaking one thread per deployment that ever served a streaming request.
     */
    private static final String[] LISTENER_CLASSES = {
            "org.wildfly.a2a.jakarta.common.SSESubscriberLifecycleListener"
    };

    @Override
    public void deploy(DeploymentPhaseContext deploymentPhaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = deploymentPhaseContext.getDeploymentUnit();

        // Only process web deployments - the merged metadata is what Undertow and the web component processor read
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        JBossWebMetaData webMetaData = warMetaData != null ? warMetaData.getMergedJBossWebMetaData() : null;
        if (webMetaData == null) {
            A2ALogger.ROOT_LOGGER.debugf("No web metadata for %s, skipping A2A listener registration", deploymentUnit.getName());
            return;
        }

        // The deployment's class loader does not exist yet in PARSE, but the module set of a provisioned server is
        // static, so the boot module loader answers just as well
        Module module = loadJakartaCommonModule();
        if (module == null) {
            A2ALogger.ROOT_LOGGER.debugf("Module %s is not present, skipping A2A listener registration for deployment '%s'",
                    JAKARTA_COMMON_MODULE, deploymentUnit.getName());
            return;
        }

        List<ListenerMetaData> listeners = webMetaData.getListeners();
        if (listeners == null) {
            listeners = new ArrayList<>();
            webMetaData.setListeners(listeners);
        }

        for (String listenerClass : LISTENER_CLASSES) {
            // A deployment that bundles a2a-jakarta itself has the listener registered by annotation scanning already
            if (isRegistered(listeners, listenerClass) || !isClassAvailable(module, listenerClass)) {
                continue;
            }
            A2ALogger.ROOT_LOGGER.debugf("Registering servlet context listener: %s", listenerClass);
            ListenerMetaData listener = new ListenerMetaData();
            listener.setListenerClass(listenerClass);
            listeners.add(listener);
        }
    }

    private Module loadJakartaCommonModule() {
        try {
            return Module.getBootModuleLoader().loadModule(JAKARTA_COMMON_MODULE);
        } catch (ModuleLoadException e) {
            return null;
        }
    }

    private boolean isRegistered(List<ListenerMetaData> listeners, String listenerClass) {
        for (ListenerMetaData listener : listeners) {
            if (listenerClass.equals(listener.getListenerClass())) {
                return true;
            }
        }
        return false;
    }

    private boolean isClassAvailable(Module module, String className) {
        return module.getClassLoader().getResource(className.replace('.', '/') + ".class") != null;
    }
}
