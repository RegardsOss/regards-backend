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
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * In this implementation, we choose to simply evict the cache for a tenant in response to "create", "delete" and "update" events.<br>
 * Note that we might achieve more subtle, per user, eviction.
 *
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class AccessGroupCache implements IAccessGroupCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessGroupCache.class);

    private final IProjectUsersClient projectUsersClient;

    private final IAccessGroupClient accessGroupClient;

    public AccessGroupCache(IProjectUsersClient projectUsersClient, IAccessGroupClient accessGroupClient) {
        this.projectUsersClient = projectUsersClient;
        this.accessGroupClient = accessGroupClient;
    }

    @Override
    public List<AccessGroup> getAccessGroups(String email, String tenant) {
        List<AccessGroup> accessGroups = new ArrayList<>();
        try {
            FeignSecurityManager.asSystem();
            ProjectUser projectUser = HateoasUtils.unwrap(projectUsersClient.retrieveProjectUserByEmail(email)
                                                                            .getBody());
            if (projectUser != null) {
                projectUser.getAccessGroups()
                           .forEach(accessGroup -> accessGroups.add(HateoasUtils.unwrap(accessGroupClient.retrieveAccessGroup(
                               accessGroup).getBody())));
            } else {
                accessGroups.addAll(HateoasUtils.retrieveAllPages(100,
                                                                  pageable -> accessGroupClient.retrieveAccessGroupsList(
                                                                      true,
                                                                      pageable.getPageNumber(),
                                                                      pageable.getPageSize())));
            }
        } finally {
            FeignSecurityManager.reset();
        }

        return accessGroups;
    }

    @Override
    public void cleanAccessGroups(String email, String tenant) {
        LOGGER.debug("Rejecting group cache for user {} and tenant {}", email, tenant);
    }

}
