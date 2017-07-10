/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.dataaccess.client.IAccessGroupClient;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;

/**
 * In this implementation, we choose to simply evict the cache for a tenant in response to "create", "delete" and "update" events.<br>
 * Note that we might achieve more subtile, per user, eviction.
 *
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class AccessGroupClientService implements IAccessGroupClientService {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessGroupClientService.class);

    /**
     * Feign client returning all access groups for a user. Autowired by Spring.
     */
    private final IUserClient userClient;

    /**
     * Allows to retrieve all public groups
     */
    private final IAccessGroupClient groupClient;

    public AccessGroupClientService(IUserClient userClient, IAccessGroupClient groupClient) {
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
            Collection<Resource<AccessGroup>> content = userClient.retrieveAccessGroupsOfUser(pUserEmail, 0, 0)
                    .getBody().getContent();
            return HateoasUtils.unwrapCollection(content);
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

            Collection<Resource<AccessGroup>> content = groupClient.retrieveAccessGroupsList(true, 0, 0).getBody()
                    .getContent();
            return HateoasUtils.unwrapCollection(content);
        } finally {
            FeignSecurityManager.reset();
        }
    }

    @Override
    public void cleanPublicAccessGroups(String tenant) {
        LOGGER.debug("Rejecting public group cache for tenant {}", tenant);
    }

}
