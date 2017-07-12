package fr.cnes.regards.framework.gson.autoconfigure;
/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.google.gson.TypeAdapterFactory;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 * Test auto configuration
 *
 * @author msordi
 *
 */
public class GsonAutoConfigurationTest {

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
        loadApplicationContext(EmptyConfiguration.class);
        Map<String, TypeAdapterFactory> factories = context.getBeansOfType(TypeAdapterFactory.class);
        Assert.assertEquals(0, factories.size());
    }

    @Test
    public void testCustomAutoConfiguration() {
        loadApplicationContext(CustomConfiguration.class);
        Map<String, TypeAdapterFactory> factories = context.getBeansOfType(TypeAdapterFactory.class);
        Assert.assertEquals(1, factories.size());
    }

    private void loadApplicationContext(Class<?> pConfig, String... pPairs) {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        EnvironmentTestUtils.addEnvironment(context, pPairs);
        context.register(pConfig);
        context.register(GsonAutoConfiguration.class);
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
     * Define custom GSON factory as bean
     *
     * @author msordi
     *
     */
    @Configuration
    static class CustomConfiguration {

        @Bean
        public PolymorphicTypeAdapterFactory<String> customFactory() {
            return PolymorphicTypeAdapterFactory.of(String.class);
        }
    }

}
