/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.unrelated;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

/**
 * JAX-RS resource of the deployment that does not use A2A.
 */
@Path("/plain")
public class PlainResource {

    @Inject
    PlainBean bean;

    @GET
    public String greeting() {
        return bean.greeting();
    }

    /**
     * Reports whether the given class is visible to this deployment.
     */
    @GET
    @Path("/visible")
    public boolean visible(@QueryParam("class") String className) {
        try {
            Class.forName(className, false, PlainResource.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
