/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service.cache;

import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.models.domain.event.AttributeModelCreated;
import fr.cnes.regards.modules.models.domain.event.AttributeModelDeleted;

/**
 * {@link IAttributeModelCache} implementation
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
     * AMPQ messages subscriber
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Cached attribute models
     */
    Optional<List<AttributeModel>> attributeModels;

    /**
     * Creates a new instance of the service with passed services/repos
     *
     * @param pAttributeModelClient
     *            Service returning the list of attribute models and keeping the list up-to-date
     */
    public AttributeModelCache(IAttributeModelClient pAttributeModelClient) {
        super();
        attributeModelClient = pAttributeModelClient;
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
    public List<AttributeModel> getAttributeModels() {
        return HateoasUtils.unwrapList(attributeModelClient.getAttributes(null, null).getBody());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.cache.IAttributeModelCache#findAllAndCache()
     */
    @Override
    public List<AttributeModel> getAttributeModelsThenCache() {
        return HateoasUtils.unwrapList(attributeModelClient.getAttributes(null, null).getBody());
    }

    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.cache.IAttributeModelCache#findByName(java.lang.String)
     */
    /*
     * (non-Javadoc)
     *
     * @see fr.cnes.regards.modules.search.service.cache.IAttributeModelCache#findByName(java.lang.String)
     */
    @Override
    public AttributeModel findByName(String pName) throws EntityNotFoundException {
        return getAttributeModels().stream().filter(el -> el.getName().equals(pName)).findFirst()
                .orElseThrow(() -> new EntityNotFoundException(pName, AttributeModel.class));
    }

    /**
     * Handle {@link AttributeModel} creation
     *
     * @author Xavier-Alexandre Brochard
     *
     */
    private class CreatedHandler implements IHandler<AttributeModelCreated> {

        @Override
        public void handle(TenantWrapper<AttributeModelCreated> pWrapper) {
            LOGGER.info("New attribute model created, refreshing the cache", pWrapper.getContent().getAttributeName());
            getAttributeModelsThenCache();
        }
    }

    /**
     * Handle {@link AttributeModel} deletion
     *
     * @author Xavier-Alexandre Brochard
     *
     */
    private class DeletedHandler implements IHandler<AttributeModelDeleted> {

        @Override
        public void handle(TenantWrapper<AttributeModelDeleted> pWrapper) {
            LOGGER.info("New attribute model deleted, refreshing the cache", pWrapper.getContent().getAttributeName());
            getAttributeModelsThenCache();
        }
    }

}
