/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.a2a.test.integration.rest.unrelated;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * CDI bean of the deployment that does not use A2A.
 */
@ApplicationScoped
public class PlainBean {

    public String greeting() {
        return "ok";
    }
}
