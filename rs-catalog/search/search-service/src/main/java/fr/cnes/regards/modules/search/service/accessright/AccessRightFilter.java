/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
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
    public ICriterion addUserGroups(ICriterion pCriterion) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        FeignSecurityManager.asSystem();
        try {
            if (!projectUserClient.isAdmin(userEmail).getBody()) {
                List<AccessGroup> accessGroups = cache.getAccessGroups(userEmail, runtimeTenantResolver.getTenant());

                // Create a list with all group criterions plus the initial criterion
                List<ICriterion> rootWithGroups = accessGroups.stream().map(GROUP_TO_CRITERION)
                        .collect(Collectors.toList());
                rootWithGroups.add(pCriterion);
                // Build the final "and" criterion
                return ICriterion.and(rootWithGroups);
            }
            return pCriterion;
        } finally {
            FeignSecurityManager.reset();
        }
    }

}
