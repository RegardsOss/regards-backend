/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.accessright;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.indexer.domain.criterion.ICriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.StringMatchCriterion;
import fr.cnes.regards.modules.search.domain.Terms;
import fr.cnes.regards.modules.search.service.cache.accessgroup.IAccessGroupCache;

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
    private final IAccessGroupCache cache;

    /**
     * Get current tenant at runtime. Autworied by Spring.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * @param pCache
     * @param pRuntimeTenantResolver
     */
    public AccessRightFilter(IAccessGroupCache pCache, IRuntimeTenantResolver pRuntimeTenantResolver) {
        super();
        cache = pCache;
        runtimeTenantResolver = pRuntimeTenantResolver;
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
        String pUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        List<AccessGroup> accessGroups = cache.getAccessGroups(pUserEmail, runtimeTenantResolver.getTenant());

        // Create a list with all group criterions plus the initial criterion
        List<ICriterion> rootWithGroups = accessGroups.stream().map(GROUP_TO_CRITERION).collect(Collectors.toList());
        rootWithGroups.add(pCriterion);

        // Build the final "and" criterion
        return ICriterion.and(rootWithGroups);
    }

}
