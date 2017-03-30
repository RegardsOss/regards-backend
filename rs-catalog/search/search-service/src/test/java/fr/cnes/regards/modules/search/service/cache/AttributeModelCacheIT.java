/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.collect.Lists;

import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
 * Test verifying {@link IAttributeModelCache} caching facilities.
 *
 * @author Xavier-Alexandre Brochard
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class AttributeModelCacheIT {

    @Configuration
    @EnableCaching
    static class Config {

        // Simulating the caching configuration
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("attributemodels");
        }

        @Bean
        IAttributeModelCache cache() {
            return Mockito.mock(IAttributeModelCache.class);
        }

    }

    /**
     * Spring cache manager
     */
    @Autowired
    CacheManager manager;

    /**
     * Cache under test
     */
    @Autowired
    IAttributeModelCache cache;

    @SuppressWarnings("unchecked")
    @Test
    public void getAttributeModelsShouldBeCached() {

        List<AttributeModel> first = Lists.newArrayList();
        List<AttributeModel> second = Lists.newArrayList();

        // Set up the mock to return *different* objects for the first and second call
        Mockito.when(cache.getAttributeModels()).thenReturn(first, second);

        // First invocation returns object returned by the method
        List<AttributeModel> result = cache.getAttributeModels();
        Assert.assertThat(result, CoreMatchers.is(first));

        // Second invocation should return cached value, *not* second (as set up above)
        result = cache.getAttributeModels();
        Assert.assertThat(result, CoreMatchers.is(first));

        // Verify cache method was invoked once
        Mockito.verify(cache, Mockito.times(1)).getAttributeModels();
        Assert.assertThat(manager.getCache("attributemodels").get(SimpleKey.EMPTY),
                          CoreMatchers.is(CoreMatchers.notNullValue()));

        // Third invocation with different key is triggers the second invocation of the repo method
        result = cache.getAttributeModels();
        Assert.assertThat(result, CoreMatchers.is(second));
    }
}