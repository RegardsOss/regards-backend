/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.security.autoconfigure;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import fr.cnes.regards.framework.multitenant.autoconfigure.MultitenantAutoConfiguration;
import fr.cnes.regards.framework.security.controller.SecurityResourcesController;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.endpoint.IPluginResourceManager;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.filter.JWTAuthenticationProvider;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;

/**
 * @author msordi
 */
public class MethodSecurityAutoConfigurationTest {

    /**
     * Web application context
     */
    private AnnotationConfigWebApplicationContext context;

    @After
    public void close() {
        if (this.context != null) {
            this.context.close();
        }
    }

    /**
     * Check for security autoconfigure
     */
    @Test
    public void testMethodConfiguration() {
        this.context = new AnnotationConfigWebApplicationContext();
        this.context.setServletContext(new MockServletContext());
        this.context.register(SecurityVoterAutoConfiguration.class, MultitenantAutoConfiguration.class,
                              MethodSecurityAutoConfiguration.class, MethodAuthorizationServiceAutoConfiguration.class,
                              WebSecurityAutoConfiguration.class, JWTService.class, SubscriberMock.class);
        this.context.refresh();
        Assertions.assertThat(this.context.getBean(IAuthoritiesProvider.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(MethodAuthorizationService.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(IPluginResourceManager.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(SecurityResourcesController.class)).isNotNull();
        Assertions.assertThat(this.context.getBean(JWTAuthenticationProvider.class)).isNotNull();

    }

}
