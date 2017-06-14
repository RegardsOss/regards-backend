/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.search.domain.Terms;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupClientService;

/**
 * Implementation of {@link IAccessRightFilter}.
 *
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
    private final IAccessGroupClientService cache;

    /**
     * Get current tenant at runtime. Autworied by Spring.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProjectUsersClient projectUserClient;

    /**
    * Constructor
    * @param pCache the cache providing access groups
    * @param pRuntimeTenantResolver get current tenant at runtime
    * @param pProjectUserClient Feign client for project users
    */
    public AccessRightFilter(IAccessGroupClientService pCache, IRuntimeTenantResolver pRuntimeTenantResolver,
            IProjectUsersClient pProjectUserClient) {
        super();
        cache = pCache;
        runtimeTenantResolver = pRuntimeTenantResolver;
        projectUserClient = pProjectUserClient;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * fr.cnes.regards.modules.search.service.accessright.IAccessRightFilter#addGroupFilter(fr.cnes.regards.modules.
     * indexer.domain.criterion.ICriterion)
     */
    @Override
    public ICriterion addAccessRights(ICriterion userCriterion) throws AccessRightFilterException {

        // Retrieve current user from security context
        String userEmail = SecurityUtils.getActualUser();
        Assert.notNull(userEmail, "No user found!");

        try {
            FeignSecurityManager.asSystem();
            if (!projectUserClient.isAdmin(userEmail).getBody()) {
                List<AccessGroup> accessGroups = cache.getAccessGroups(userEmail, runtimeTenantResolver.getTenant());

                // Throw an error if no access group
                if ((accessGroups == null) || accessGroups.isEmpty()) {
                    String errorMessage = String.format(
                                                        "Cannot set access right filter cause user %s does not have any access group",
                                                        userEmail);
                    LOGGER.error(errorMessage);
                    throw new AccessRightFilterException(errorMessage);
                }

                List<ICriterion> searchCriterion = new ArrayList<>();

                // Add security filter
                accessGroups.forEach(accessGroup -> searchCriterion.add(GROUP_TO_CRITERION.apply(accessGroup)));
                // Add user criterion
                if (userCriterion != null) {
                    searchCriterion.add(userCriterion);
                }
                // Build the final "and" criterion
                return ICriterion.and(searchCriterion);
            }
            return userCriterion;
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
