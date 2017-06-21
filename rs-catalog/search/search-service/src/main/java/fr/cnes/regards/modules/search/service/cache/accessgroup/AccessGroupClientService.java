/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache.accessgroup;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.dataaccess.client.IUserClient;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dataaccess.domain.accessgroup.event.AccessGroupEvent;

/**
 * In this implementation, we choose to simply evict the cache for a tenant in response to "create", "delete" and "update" events.<br>
 * Note that we might achieve more subtile, per user, eviction.
 *
 * @author Xavier-Alexandre Brochard
 * @author oroussel
 */
@Service
@MultitenantTransactional
public class AccessGroupClientService implements IAccessGroupClientService, IHandler<AccessGroupEvent> {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessGroupClientService.class);

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
     */
    public AccessGroupClientService(IUserClient pUserClient, ISubscriber pSubscriber) {
        super();
        userClient = pUserClient;
        subscriber = pSubscriber;
    }

    /**
     * Subscribe to events
     */
    @PostConstruct
    private void subscribeToEvents() {
        subscriber.subscribeTo(AccessGroupEvent.class, this);
    }

    /**
     * AccessGroupEvent handler in order to refresh cache
     */
    @Override
    @CacheEvict(value = "accessgroups", key = "#pWrapper.getTenant()")
    public void handle(TenantWrapper<AccessGroupEvent> pWrapper) {
        LOGGER.info("New access group created, refreshing the cache", pWrapper.getContent().getAccessGroup().getName());

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

}
