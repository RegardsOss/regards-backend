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

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;

/**
 * In this implementation, we choose to repopulate (and not only evict) the cache for a tenant in response to "create" and "delete" events.<br>
 * This way the cache "anticipates" by repopulating immediately instead of waiting for the next user call.
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

    //    /**
    //     * Subscribe to events
    //     */
    //    @PostConstruct
    //    private void subscribeToEvents() {
    //        subscriber.subscribeTo(AttributeModelCreated.class, new CreatedHandler());
    //        subscriber.subscribeTo(AttributeModelDeleted.class, new DeletedHandler());
    //    }

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

    //    /**
    //     * Handle {@link AttributeModel} creation
    //     *
    //     * @author Xavier-Alexandre Brochard
    //     */
    //    private class CreatedHandler implements IHandler<AttributeModelCreated> {
    //
    //        @Override
    //        public void handle(TenantWrapper<AttributeModelCreated> pWrapper) {
    //            LOGGER.info("New attribute model created, refreshing the cache", pWrapper.getContent().getAttributeName());
    //            getAttributeModelsThenCache(pWrapper.getTenant());
    //        }
    //    }
    //
    //    /**
    //     * Handle {@link AttributeModel} deletion
    //     *
    //     * @author Xavier-Alexandre Brochard
    //     */
    //    private class DeletedHandler implements IHandler<AttributeModelDeleted> {
    //
    //        @Override
    //        public void handle(TenantWrapper<AttributeModelDeleted> pWrapper) {
    //            LOGGER.info("New attribute model deleted, refreshing the cache", pWrapper.getContent().getAttributeName());
    //            getAttributeModelsThenCache(pWrapper.getTenant());
    //        }
    //    }

}
