/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache;

import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
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

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupClientService;

/**
 * Test verifying {@link IAccessGroupClientService} caching facilities.<br>
 * Widely inspired by Oliver Gierke (lead dev. of Spring Data project) example below.
 *
 * @see http://stackoverflow.com/questions/24221569/how-to-test-springs-declarative-caching-support-on-spring-data-repositories
 * @author Xavier-Alexandre Brochard
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@Ignore
public class AccessGroupClientServiceIT {

    @Configuration
    @EnableCaching
    static class Config {

        // Simulating the caching configuration.
        // Actually, Spring auto-configures a suitable CacheManager according to the implementation
        // as long as the caching support is enabled via the @EnableCaching annotation.
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("accessgroups");
        }

        @Bean
        IAccessGroupClientService cache() {
            return Mockito.mock(IAccessGroupClientService.class);
        }

    }

    /**
     * Spring cache manager
     */
    @Autowired
    private CacheManager manager;

    /**
     * Cache under test
     */
    @Autowired
    private IAccessGroupClientService cache;

    @SuppressWarnings("unchecked")
    @Test
    public void getAccessGroupsShouldBeCached() {
        // Define the current tenant and current user's email
        String TENANT = "tenant";
        String EMAIL = "test@email.com";

        // Set up the mock to return *different* objects for the first and second call
        List<AccessGroup> first = Lists.newArrayList();
        List<AccessGroup> second = Lists.newArrayList();
        Mockito.when(cache.getAccessGroups(EMAIL, TENANT)).thenReturn(first, second);

        // First invocation returns object returned by the method
        List<AccessGroup> result = cache.getAccessGroups(EMAIL, TENANT);
        Assert.assertThat(result, CoreMatchers.is(first));

        // Second invocation should return cached value, *not* second (as set up above)
        result = cache.getAccessGroups(EMAIL, TENANT);
        Assert.assertThat(result, CoreMatchers.is(first));

        // Verify cache method was invoked once
        Mockito.verify(cache, Mockito.times(1)).getAccessGroups(EMAIL, TENANT);
        Assert.assertThat(manager.getCache("accessgroups").get(new SimpleKey(EMAIL, TENANT)),
                          CoreMatchers.is(CoreMatchers.notNullValue()));

        // Third invocation triggers the second invocation of the repo method (because the stub was configured so)
        result = cache.getAccessGroups(EMAIL, TENANT);
        Assert.assertThat(result, CoreMatchers.is(second));
    }

    @Test
    @Purpose("Check that the cache is multitenant")
    public void checkThatTheCacheIsMultiTenant() {
        // Define two tenants and the current user's email
        String TENANT_A = "tenantA";
        String TENANT_B = "tenantB";
        String EMAIL = "test@email.com";

        // Populate the cache
        cache.getAccessGroups(EMAIL, TENANT_A);
        cache.getAccessGroups(EMAIL, TENANT_B);

        // Verify we have an entry in the cache for each tenant
        Assert.assertThat(manager.getCache("accessgroups").get(new SimpleKey(EMAIL, TENANT_A)),
                          CoreMatchers.is(CoreMatchers.notNullValue()));
        Assert.assertThat(manager.getCache("accessgroups").get(new SimpleKey(EMAIL, TENANT_B)),
                          CoreMatchers.is(CoreMatchers.notNullValue()));
    }
}