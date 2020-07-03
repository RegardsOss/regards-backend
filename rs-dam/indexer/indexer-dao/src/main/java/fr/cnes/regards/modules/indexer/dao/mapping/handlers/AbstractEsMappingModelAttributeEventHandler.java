package fr.cnes.regards.modules.indexer.dao.mapping.handlers;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.indexer.dao.mapping.AttributeDescription;
import fr.cnes.regards.modules.indexer.dao.mapping.IEsMappingUpdateService;
import fr.cnes.regards.modules.model.domain.event.AbstractAttributeModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;

public abstract class AbstractEsMappingModelAttributeEventHandler<T extends AbstractAttributeModelEvent>
        implements ApplicationListener<ApplicationReadyEvent>, IHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractEsMappingModelAttributeEventHandler.class);

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IEsMappingUpdateService mappingUpdater;

    abstract Class<T> eventType();

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(eventType(), this);
    }

    @Override
    public void handle(String tenant, T message) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            mappingUpdater.addAttributeToIndexMapping(tenant, new AttributeDescription(message));
        }
        catch(ModuleException e) {
            LOGGER.error("Could not add mapping for {} in tenant {}", message, tenant, e);
        }
    }

}
