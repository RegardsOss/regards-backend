/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;

/**
 * Provider for {@link AccessGroup}s with caching facilities.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessGroupCache { //NOSONAR

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