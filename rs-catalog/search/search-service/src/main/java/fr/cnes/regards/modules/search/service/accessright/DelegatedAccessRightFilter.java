/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.accessright;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("delegated-security")
public class DelegatedAccessRightFilter extends AccessRightFilter implements IAccessRightFilter {

    public DelegatedAccessRightFilter(IAuthenticationResolver authResolver,
                                      IAccessGroupCache cache,
                                      IRuntimeTenantResolver runtimeTenantResolver,
                                      IProjectUsersClient projectUsersClient) {
        super(authResolver, cache, runtimeTenantResolver, projectUsersClient);
    }

    /**
     * @return access groups from security context
     */
    @Override
    protected Set<String> getAccessGroups() {
        Set<String> accessGroups = cache.getAccessGroups(authResolver.getUser(), runtimeTenantResolver.getTenant())
                                        .stream()
                                        .map(AccessGroup::getName)
                                        .collect(Collectors.toSet());
        if (authResolver.getAccessGroups() != null) {
            accessGroups.addAll(authResolver.getAccessGroups());

        }
        return accessGroups;
    }
}
