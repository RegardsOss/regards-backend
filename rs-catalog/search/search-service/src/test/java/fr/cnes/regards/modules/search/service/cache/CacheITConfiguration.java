/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache;

import org.mockito.Mockito;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.modules.search.service.cache.attributemodel.IAttributeModelCache;

@Configuration
@EnableCaching class CacheITConfiguration {

    // Simulating the caching configuration.
    // Actually, Spring auto-configures a suitable CacheManager according to the implementation
    // as long as the caching support is enabled via the @EnableCaching annotation.
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("attributemodels");
    }

    @Bean
    public IAttributeModelCache cache() {
        return Mockito.mock(IAttributeModelCache.class);
    }

}