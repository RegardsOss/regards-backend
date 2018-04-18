/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.event.DataStorageEvent;
import fr.cnes.regards.modules.storage.domain.event.StorageAction;
import fr.cnes.regards.modules.storage.domain.event.StorageEventType;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.service.job.StorageJobProgressManager;

/**
 * Handler for DataStorageEvent events. This events are sent by the {@link StorageJobProgressManager} associated
 * to the {@link IDataStorage} plugins. After each {@link StorageDataFile} stored, deleted or restored a
 * {@link DataStorageEvent}
 * should be sent thought the {@link StorageJobProgressManager}.
 * @author Sylvain Vissiere-Guerinet
 * @author Sébastien Binda
 */
@Component
@RegardsTransactional
public class DataStorageEventHandler implements IHandler<DataStorageEvent>, IDataStorageEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DataStorageEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IDataStorageService dataStorageService;

    @Autowired
    private ISubscriber subscriber;

    /**
     * The spring application name ~= microservice type
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.service.IPlop#onApplicationEvent(org.springframework.boot.context.event.ApplicationReadyEvent)
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Subscribe to events on {@link StorageDataFile} changes.
        subscriber.subscribeTo(DataStorageEvent.class, this);
    }

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.service.IPlop#handle(fr.cnes.regards.framework.amqp.domain.TenantWrapper)
     */
    @Override
    public void handle(TenantWrapper<DataStorageEvent> wrapper) {
        LOG.debug("New DataStorageEvent received - action:{}/{} - pluginConfId:{} - dataFileId:{}",
                  wrapper.getContent().getStorageAction().toString(), wrapper.getContent().getType(),
                  wrapper.getContent().getStorageConfId(), wrapper.getContent().getDataFileId());
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        DataStorageEvent event = wrapper.getContent();
        StorageAction action = event.getStorageAction();
        StorageEventType type = event.getType();
        switch (action) {
            case STORE:
                dataStorageService.handleStoreAction(type, event);
                break;
            case DELETION:
                dataStorageService.handleDeletionAction(type, event);
                break;
            case RESTORATION:
                dataStorageService.handleRestorationAction(type, event);
                break;
            default:
                throw new EnumConstantNotPresentException(StorageAction.class, action.toString());
        }
        runtimeTenantResolver.clearTenant();
    }
}
