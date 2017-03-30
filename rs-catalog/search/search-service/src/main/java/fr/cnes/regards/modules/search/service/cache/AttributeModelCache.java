/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.models.domain.event.AttributeModelDeleted;

/**
 * In this implementation, we choose to repopulate (and not only evict) the cache for a tenant in response to "create" and "delete" events.<br>
 * This way the cache "anticipates" by repopulating immediately instead of waiting for the next user call.
 *
 * @author Xavier-Alexandre Brochard
 */
@Service
public class AttributeModelCache implements IAttributeModelCache {

    /**
     * Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeModelCache.class);

    /**
     * Feign client for rs-dam {@link AttributeModel} controller. Autowired by Spring.
     */
    private final IAttributeModelClient attributeModelClient;

    /**
     * AMPQ messages subscriber. Autowired by Spring.
     */
    private final ISubscriber subscriber;

    /**
     * Retrieve the current tenant at runtime. Autowired by Spring.
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Creates a new instance of the service with passed services/repos
     *
     * @param pAttributeModelClient Service returning the list of attribute models and keeping the list up-to-date
     * @param pSubscriber the AMQP events subscriber
     * @param pRuntimeTenantResolver the runtime tenant resolver
     */
    public AttributeModelCache(IAttributeModelClient pAttributeModelClient, ISubscriber pSubscriber,
            IRuntimeTenantResolver pRuntimeTenantResolver) {
        super();
        attributeModelClient = pAttributeModelClient;
        subscriber = pSubscriber;
        runtimeTenantResolver = pRuntimeTenantResolver;
    }

    /**
     * Subscribe to events
     */
    @PostConstruct
    private void subscribeToEvents() {
        subscriber.subscribeTo(AttributeModelCreated.class, new CreatedHandler());
        subscriber.subscribeTo(AttributeModelDeleted.class, new DeletedHandler());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.cache.IAttributeModelCache#findAll()
     */
    @Override
    public List<AttributeModel> getAttributeModels(String pTenant) {
        return HateoasUtils.unwrapList(attributeModelClient.getAttributes(null, null).getBody());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.cache.IAttributeModelCache#findAllAndCache()
     */
    @Override
    public List<AttributeModel> getAttributeModelsThenCache(String pTenant) {
        return HateoasUtils.unwrapList(attributeModelClient.getAttributes(null, null).getBody());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.cache.IAttributeModelCache#findByName(java.lang.String)
     */
    @Override
    public AttributeModel findByName(String pName) throws EntityNotFoundException {
        String tenant = runtimeTenantResolver.getTenant();
        return getAttributeModels(tenant).stream().filter(el -> el.getName().equals(pName)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(pName, AttributeModel.class));
    }

    /**
     * Handle {@link AttributeModel} creation
     *
     * @author Xavier-Alexandre Brochard
     */
    private class CreatedHandler implements IHandler<AttributeModelCreated> {

        @Override
        public void handle(TenantWrapper<AttributeModelCreated> pWrapper) {
            LOGGER.info("New attribute model created, refreshing the cache", pWrapper.getContent().getAttributeName());
            getAttributeModelsThenCache(pWrapper.getTenant());
        }
    }

    /**
     * Handle {@link AttributeModel} deletion
     *
     * @author Xavier-Alexandre Brochard
     */
    private class DeletedHandler implements IHandler<AttributeModelDeleted> {

        @Override
        public void handle(TenantWrapper<AttributeModelDeleted> pWrapper) {
            LOGGER.info("New attribute model deleted, refreshing the cache", pWrapper.getContent().getAttributeName());
            getAttributeModelsThenCache(pWrapper.getTenant());
        }
    }

}
