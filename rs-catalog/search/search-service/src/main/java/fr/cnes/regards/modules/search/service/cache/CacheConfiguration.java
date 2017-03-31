/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Enable caching support in Spring.
 *
 * @author Xavier-Alexandre Brochard
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    // Is is needed or not?
    //    @Bean
    //    public CacheManager cacheManager() {
    //        return new GuavaCacheManager("accessgroups");
    //    }

}
