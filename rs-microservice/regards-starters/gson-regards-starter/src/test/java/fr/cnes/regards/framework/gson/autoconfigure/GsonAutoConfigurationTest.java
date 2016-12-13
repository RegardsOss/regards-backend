package fr.cnes.regards.framework.gson.autoconfigure;
/*
 * LICENSE_PLACEHOLDER
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
