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
package fr.cnes.regards.framework.multitenant.autoconfigure;

import java.util.Set;
import java.util.TreeSet;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Test auto configuration
 *
 * @author msordi
 *
 */
public class MultitenantAutoConfigurationTest {

    /**
     * Custom tenant
     */
    private static final String CUSTOM_TENANT = "CUSTOM_TENANT";

    /**
     * Application context
     */
    private AnnotationConfigWebApplicationContext context;

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void testDefaultAutoConfiguration() {
        final String project1 = "PROJECT1";
        final String project2 = "PROJECT2";
        loadApplicationContext(EmptyConfiguration.class, "regards.tenants=" + project1 + ", " + project2);
        final ITenantResolver tenantResolver = context.getBean(ITenantResolver.class);
        Assert.assertNotNull(tenantResolver);
        Assert.assertEquals(2, tenantResolver.getAllTenants().size(), 2);
        Assert.assertTrue(tenantResolver.getAllTenants().contains(project1));
        Assert.assertTrue(tenantResolver.getAllTenants().contains(project2));
    }

    @Test
    public void testDefaultNoPropertyAutoConfiguration() {
        loadApplicationContext(EmptyConfiguration.class);
        final ITenantResolver tenantResolver = context.getBean(ITenantResolver.class);
        Assert.assertNotNull(tenantResolver);
        Assert.assertTrue(tenantResolver.getAllTenants().isEmpty());
    }

    @Test
    public void testCustomAutoConfiguration() {
        loadApplicationContext(CustomConfiguration.class);
        final ITenantResolver tenantResolver = context.getBean(ITenantResolver.class);
        Assert.assertTrue(tenantResolver instanceof CustomConfiguration.CustomTenantResolver);
        Assert.assertTrue(tenantResolver.getAllTenants().contains(CUSTOM_TENANT));
    }

    private void loadApplicationContext(Class<?> pConfig, String... pPairs) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        EnvironmentTestUtils.addEnvironment(context, pPairs);
        context.register(pConfig);
        context.register(MultitenantAutoConfiguration.class);
        context.refresh();
    }

    /**
     * Empty configuration
     *
     * @author msordi
     *
     */
    @Configuration
    static class EmptyConfiguration {
    }

    /**
     * Custom tenant resolver configuration
     *
     * @author msordi
     *
     */
    @Configuration
    static class CustomConfiguration {

        @Bean
        public ITenantResolver customTenantResolver() {
            return new CustomTenantResolver();
        }

        /**
         * Custom tenant resolver
         *
         * @author msordi
         *
         */
        private class CustomTenantResolver implements ITenantResolver {

            @Override
            public Set<String> getAllTenants() {
                final Set<String> set = new TreeSet<>();
                set.add(CUSTOM_TENANT);
                return set;
            }

            @Override
            public Set<String> getAllActiveTenants() {
                return getAllTenants();
            }
        }
    }

}
