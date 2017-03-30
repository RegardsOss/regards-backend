/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import java.util.List;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;

/**
 * Provider for {@link AccessGroup}s with caching facilities.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessGroupCache {

    /**
     * The call will first check the cache "accessgroups" before actually invoking the method and then caching the
     * result.<br>
     * Each tenant will add an entry to the cache, so the cache is multitenant.
     * @param the user whom we want the access groups
     * @param pTenant the tenant. Only here for auto-building a multitenant cache, and might not be used in the implementation.
     * @return the list of access groups
     */
    @Cacheable(value = "accessgroups")
    List<AccessGroup> getAccessGroups(String pUserEmail, String pTenant);

    /**
     * *ONLY HERE FOR CACHING SYSTEM, DO NOT USE MANUALLY*<br>
     * The call will first check the cache "accessgroups" before actually invoking the method and then caching the
     * result.<br>
     * Each tenant will add an entry to the cache, so the cache is multitenant.
     * @param the user whom we want the access groups
     * @param pTenant the tenant. Only here for auto-building a multitenant cache, and might not be used in the implementation.
     * @return the list of access groups
     */
    @CachePut(value = "accessgroups")
    List<AccessGroup> getAccessGroupsThenCache(String pUserEmail, String pTenant);

}