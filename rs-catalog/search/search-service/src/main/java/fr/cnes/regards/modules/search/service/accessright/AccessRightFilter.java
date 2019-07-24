/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.search.domain.Terms;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;

/**
 * Implementation of {@link IAccessRightFilter}.
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AccessRightFilter implements IAccessRightFilter {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessRightFilter.class);

    /**
     * Function which creates a {@link StringMatchCriterion} from an {@link AccessGroup}.
     */
    private static final Function<AccessGroup, ICriterion> GROUP_TO_CRITERION = group -> ICriterion
            .eq(Terms.GROUPS.getName(), group.getName());

    /**
     * Provides access groups for a user with cache facilities. Autowired by Spring.
     */
    private final IAccessGroupCache cache;

    /**
     * Get current tenant at runtime. Autowired by Spring.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProjectUsersClient projectUserClient;

    private final IAuthenticationResolver authResolver;

    public AccessRightFilter(final IAuthenticationResolver authResolver, IAccessGroupCache pCache,
            IRuntimeTenantResolver pRuntimeTenantResolver, IProjectUsersClient pProjectUserClient) {
        super();
        this.authResolver = authResolver;
        this.cache = pCache;
        this.runtimeTenantResolver = pRuntimeTenantResolver;
        this.projectUserClient = pProjectUserClient;
    }

    /**
     * First, check current user role. If role is a custom one, call admin to know if its an admin role.
     * @return true if current authentified user is an admin
     */
    private boolean isAdmin() {

        // Retrieve current user from security context
        String userEmail = authResolver.getUser();
        String role = authResolver.getRole();
        Assert.notNull(userEmail, "Unknown request user!");
        Assert.notNull(role, "Unknown request user role!");

        // For default ROLE, avoid feign request
        if (role.equals(DefaultRole.ADMIN.toString()) || role.equals(DefaultRole.PROJECT_ADMIN.toString())
                || role.equals(DefaultRole.INSTANCE_ADMIN.toString())) {
            return true;
        }

        if (role.equals(DefaultRole.PUBLIC.toString()) || role.equals(DefaultRole.REGISTERED_USER.toString())) {
            return false;
        }

        // We have to check parent role ... not available in token at the moment
        // FIXME add parent role in the JWT token CLAIMS
        try {
            FeignSecurityManager.asUser(userEmail, authResolver.getRole());
            return projectUserClient.isAdmin(userEmail).getBody();
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public ICriterion addAccessRights(ICriterion userCriterion) throws AccessRightFilterException {

        if (!isAdmin()) {

            // Retrieve public groups
            Set<AccessGroup> accessGroups = new HashSet<>(
                    cache.getPublicAccessGroups(runtimeTenantResolver.getTenant()));

            // Add explicitly associated group
            accessGroups.addAll(cache.getAccessGroups(authResolver.getUser(), runtimeTenantResolver.getTenant()));

            // Throw an error if no access group
            if (accessGroups.isEmpty()) {
                String errorMessage = String
                        .format("Cannot set access right filter because user %s does not have " + "any access group",
                                authResolver.getUser());
                LOGGER.error(errorMessage);
                throw new AccessRightFilterException(errorMessage);
            }

            List<ICriterion> searchCriterion = new ArrayList<>();

            // Add security filter
            List<ICriterion> groupCriterions = new ArrayList<>();
            accessGroups.forEach(accessGroup -> groupCriterions.add(GROUP_TO_CRITERION.apply(accessGroup)));
            searchCriterion.add(ICriterion.or(groupCriterions));

            // Add user criterion (theorically, userCriterion should not be null at this point but...)
            if (userCriterion != null && !userCriterion.equals(ICriterion.all())) {
                searchCriterion.add(userCriterion);
            }

            // Build the final "and" criterion
            return ICriterion.and(searchCriterion);
        }
        return userCriterion;
    }

    // TODO : by now, is the same as previous method
    @Override
    public ICriterion addDataAccessRights(ICriterion userCriterion) throws AccessRightFilterException {

        if (!isAdmin()) {

            // Retrieve public groups
            Set<AccessGroup> accessGroups = new HashSet<>(
                    cache.getPublicAccessGroups(runtimeTenantResolver.getTenant()));

            // Add explicitly associated group
            accessGroups.addAll(cache.getAccessGroups(authResolver.getUser(), runtimeTenantResolver.getTenant()));

            // Throw an error if no access group
            if (accessGroups.isEmpty()) {
                String errorMessage = String
                        .format("Cannot set access right filter because user %s does not have " + "any access group",
                                authResolver.getUser());
                LOGGER.error(errorMessage);
                throw new AccessRightFilterException(errorMessage);
            }

            List<ICriterion> searchCriterion = new ArrayList<>();

            // Add security filter
            List<ICriterion> groupCriterions = new ArrayList<>();
            accessGroups.forEach(accessGroup -> groupCriterions.add(GROUP_TO_CRITERION.apply(accessGroup)));
            searchCriterion.add(ICriterion.or(groupCriterions));

            // Add user criterion (theorically, userCriterion should not be null at this point but...)
            if (userCriterion != null && !userCriterion.equals(ICriterion.all())) {
                searchCriterion.add(userCriterion);
            }

            // Build the final "and" criterion
            return ICriterion.and(searchCriterion);
        }
        return userCriterion;
    }

    @Override
    public Set<String> getUserAccessGroups() throws AccessRightFilterException {

        if (!isAdmin()) {

            // Retrieve public groups
            Set<AccessGroup> accessGroups = new HashSet<>(
                    cache.getPublicAccessGroups(runtimeTenantResolver.getTenant()));

            // Add explicitly associated group
            accessGroups.addAll(cache.getAccessGroups(authResolver.getUser(), runtimeTenantResolver.getTenant()));

            // Throw an error if no access group
            if (accessGroups.isEmpty()) {
                String errorMessage = String
                        .format("Cannot set access right filter because user %s does not have " + "any access group",
                                authResolver.getUser());
                LOGGER.error(errorMessage);
                throw new AccessRightFilterException(errorMessage);
            }

            return accessGroups.stream().map(AccessGroup::getName).collect(Collectors.toSet());
        }
        return null;
    }
}
