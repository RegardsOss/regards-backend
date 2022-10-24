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
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Cache only used to retrieve public groups.
 * The assignment of other groups to users is delegated to the external security system.
 */
@Service
@MultitenantTransactional
@Profile("delegated-security")
public class DelegatedAccessGroupCache extends AccessGroupCache implements IAccessGroupCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelegatedAccessGroupCache.class);

    private final IAccessGroupClient accessGroupClient;

    public DelegatedAccessGroupCache(IProjectUsersClient projectUsersClient, IAccessGroupClient accessGroupClient) {
        super(projectUsersClient, accessGroupClient);
        this.accessGroupClient = accessGroupClient;
    }

    /**
     * Retrieve public groups only
     */
    @Override
    public List<AccessGroup> getAccessGroups(String email, String tenant) {
        List<AccessGroup> accessGroups = new ArrayList<>();
        try {
            FeignSecurityManager.asSystem();
            accessGroups.addAll(HateoasUtils.retrieveAllPages(100,
                                                              pageable -> accessGroupClient.retrieveAccessGroupsList(
                                                                  true,
                                                                  pageable.getPageNumber(),
                                                                  pageable.getPageSize())));

        } finally {
            FeignSecurityManager.reset();
        }

        return accessGroups;
    }
}
