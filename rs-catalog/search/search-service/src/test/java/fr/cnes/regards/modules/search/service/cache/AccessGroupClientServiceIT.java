/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.cache;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * Test verifying {@link IAccessGroupCache} caching facilities.<br>
 * Widely inspired by Oliver Gierke (lead dev. of Spring Data project) example below.
 *
 * @author Xavier-Alexandre Brochard
 * @see "http://stackoverflow.com/questions/24221569/how-to-test-springs-declarative-caching-support-on-spring-data-repositories"
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
@Ignore
@ActiveProfiles("test")
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
        IAccessGroupCache cache() {
            return Mockito.mock(IAccessGroupCache.class);
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
    private IAccessGroupCache cache;

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