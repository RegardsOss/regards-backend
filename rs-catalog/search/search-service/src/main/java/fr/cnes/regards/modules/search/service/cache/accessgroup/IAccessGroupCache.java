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

import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

/**
 * Provider for {@link AccessGroup}s with caching facilities.
 *
 * @author Xavier-Alexandre Brochard
 */
public interface IAccessGroupCache {

    /**
     * Retrieve a user's access groups
     * The call will first check the "accessgroups" cache before actually invoking the method and then caching the result.<br>
     * Each tenant will add an entry to the cache, so the cache is multi-tenant.
     *
     * @param email  User's email
     * @param tenant The tenant. Only here for auto-building a multi-tenant cache, and might not be used in the implementation.
     * @return A list of access groups
     */
    @Cacheable(value = "accessgroups")
    List<AccessGroup> getAccessGroups(String email, String tenant);

    /**
     * Clean access groups cache for specified tenant
     *
     * @param email  User's email
     * @param tenant The tenant. Only here for auto-building a multi-tenant cache, and might not be used in the implementation.
     */
    @CacheEvict(value = "accessgroups")
    void cleanAccessGroups(String email, String tenant);

}