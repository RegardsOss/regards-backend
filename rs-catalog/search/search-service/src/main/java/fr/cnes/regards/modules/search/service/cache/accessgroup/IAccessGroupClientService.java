/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;

/**
 * Provider for {@link AccessGroup}s with caching facilities.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessGroupClientService { //NOSONAR

    /**
     * The call will first check the cache "accessgroups" before actually invoking the method and then caching the
     * result.<br>
     * Each tenant will add an entry to the cache, so the cache is multitenant.
     * @param userEmail user whom we want the access groups
     * @param tenant the tenant. Only here for auto-building a multitenant cache, and might not be used in the implementation.
     * @return the list of access groups
     */
    @Cacheable(value = "accessgroups")
    List<AccessGroup> getAccessGroups(String userEmail, String tenant);

    /**
     * Clean cache for specified tenant
     * @param tenant
     */
    @CacheEvict(value = "accessgroups")
    void cleanAccessGroups(String userEmail, String tenant);

    /**
     *
     * @param tenant tenant
     * @return list of public groups
     */
    @Cacheable(value = "publicgroups")
    List<AccessGroup> getPublicAccessGroups(String tenant);

    /**
     * Clean cache for specified tenant
     * @param tenant tenant
     */
    @CacheEvict(value = "publicgroups")
    void cleanPublicAccessGroups(String tenant);
}