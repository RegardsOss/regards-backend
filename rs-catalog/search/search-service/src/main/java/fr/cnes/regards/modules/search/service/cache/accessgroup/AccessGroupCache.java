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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IUserClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;

/**
 * In this implementation, we choose to simply evict the cache for a tenant in response to "create", "delete" and
 * "update" events.<br>
 * Note that we might achieve more subtile, per user, eviction.
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AccessGroupCache implements IAccessGroupCache {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessGroupCache.class);

    /**
     * Feign client returning all access groups for a user. Autowired by Spring.
     */
    private final IUserClient userClient;

    /**
     * Allows to retrieve all public groups
     */
    private final IAccessGroupClient groupClient;

    public AccessGroupCache(IUserClient userClient, IAccessGroupClient groupClient) {
        this.userClient = userClient;
        this.groupClient = groupClient;
    }

    @Override
    public List<AccessGroup> getAccessGroups(String pUserEmail, String pTenant) {
        return doGetAccessGroups(pUserEmail);
    }

    /**
     * Use the feign client to retrieve the access groups of the passed user.<br>
     * The method is private because it is not expected to be used directly, but via its cached facade "getAccessGroups" method.
     * @param pUserEmail the user email
     * @return the list of user's access groups
     */
    private List<AccessGroup> doGetAccessGroups(String pUserEmail) {
        try {
            // Enable system call as follow (thread safe action)
            FeignSecurityManager.asSystem();
            // Perform client call
            return HateoasUtils.retrieveAllPages(100, pageable -> userClient
                    .retrieveAccessGroupsOfUser(pUserEmail, pageable.getPageNumber(), pageable.getPageSize()));
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void cleanAccessGroups(String userEmail, String tenant) {
        LOGGER.debug("Rejecting group cache for user {} and tenant {}", userEmail, tenant);
    }

    @Override
    public List<AccessGroup> getPublicAccessGroups(String tenant) {
        try {
            FeignSecurityManager.asSystem();
            return HateoasUtils.retrieveAllPages(100, pageable -> groupClient
                    .retrieveAccessGroupsList(true, pageable.getPageNumber(), pageable.getPageSize()));
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void cleanPublicAccessGroups(String tenant) {
        LOGGER.debug("Rejecting public group cache for tenant {}", tenant);
    }

}
