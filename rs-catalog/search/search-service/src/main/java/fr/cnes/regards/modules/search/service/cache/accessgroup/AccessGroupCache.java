/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.hateoas.Resource;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupAssociationUpdated;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupCreated;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupDeleted;

/**
 * In this implementation, we choose to simply evict the cache for a tenant in response to "create", "delete" and "update" events.<br>
 * Note that we might achieve more subtile, per user, eviction.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
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
     * AMPQ messages subscriber. Autowired by Spring.
     */
    private final ISubscriber subscriber;

    /**
     * Creates a new instance of the service with passed services/repos
     *
     * @param pUserClient Feign client returning all access groups for a user
     * @param pSubscriber the AMQP events subscriber
     * @param pRuntimeTenantResolver the runtime tenant resolver
     */
    public AccessGroupCache(IUserClient pUserClient, ISubscriber pSubscriber) {
        super();
        userClient = pUserClient;
        subscriber = pSubscriber;
    }

    /**
     * Subscribe to events
     */
    @PostConstruct
    private void subscribeToEvents() {
        subscriber.subscribeTo(AccessGroupCreated.class, new CreatedHandler());
        subscriber.subscribeTo(AccessGroupDeleted.class, new DeletedHandler());
        subscriber.subscribeTo(AccessGroupAssociationUpdated.class, new AssociationUpdatedHandler());
    }

    @Override
    public List<AccessGroup> getAccessGroups(String pUserEmail, String pTenant) {
        return doGetAccessGroups(pUserEmail);
    }

    @Override
    public List<AccessGroup> getAccessGroupsThenCache(String pUserEmail, String pTenant) {
        return doGetAccessGroups(pUserEmail);
    }

    /**
     * Use the feign client to retrieve the access groups of the passed user.<br>
     * The method is private because it is not expected to be used directly, but via its cached facade "getAccessGroups" method.
     * @param pUserEmail the user email
     * @return the list of user's access groups
     */
    private List<AccessGroup> doGetAccessGroups(String pUserEmail) {
        // Enable system call as follow (thread safe action)
        FeignSecurityManager.asSystem();

        // Perform client call
        Collection<Resource<AccessGroup>> content = userClient.retrieveAccessGroupsOfUser(pUserEmail, 0, 0).getBody()
                .getContent();
        List<AccessGroup> result = HateoasUtils.unwrapCollection(content);

        // Disable system call if necessary after client request(s)
        FeignSecurityManager.reset();

        return result;
    }

    /**
     * Handle {@link AccessGroup} creation
     *
     * @author Xavier-Alexandre Brochard
     */
    private class CreatedHandler implements IHandler<AccessGroupCreated> {

        @Override
        @CacheEvict(value = "accessgroups", key = "#pWrapper.getTenant()")
        public void handle(TenantWrapper<AccessGroupCreated> pWrapper) {
            LOGGER.info("New access group created, refreshing the cache",
                        pWrapper.getContent().getAccessGroup().getName());
        }
    }

    /**
     * Handle {@link AccessGroup} deletion
     *
     * @author Xavier-Alexandre Brochard
     */
    private class DeletedHandler implements IHandler<AccessGroupDeleted> {

        @Override
        @CacheEvict(value = "accessgroups", key = "#pWrapper.getTenant()")
        public void handle(TenantWrapper<AccessGroupDeleted> pWrapper) {
            LOGGER.info("Access group deleted, refreshing the cache", pWrapper.getContent().getAccessGroup().getName());
        }
    }

    /**
     * Handle user associations/dissociations for an {@link AccessGroup}
     *
     * @author Xavier-Alexandre Brochard
     */
    private class AssociationUpdatedHandler implements IHandler<AccessGroupAssociationUpdated> {

        @Override
        @CacheEvict(value = "accessgroups", key = "#pWrapper.getTenant()")
        public void handle(TenantWrapper<AccessGroupAssociationUpdated> pWrapper) {
            LOGGER.info("Access group user associated/dissociated, refreshing the cache",
                        pWrapper.getContent().getAccessGroup().getName());
        }
    }

}
