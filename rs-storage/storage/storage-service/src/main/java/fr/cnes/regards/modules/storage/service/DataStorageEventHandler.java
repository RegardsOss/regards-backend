/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
        String tenant = wrapper.getTenant();
        runtimeTenantResolver.forceTenant(tenant);
        LOG.debug("New DataStorageEvent received - action:{}/{} - pluginConfId:{} - dataFileId:{}",
                  wrapper.getContent().getStorageAction().toString(), wrapper.getContent().getType(),
                  wrapper.getContent().getStorageConfId(), wrapper.getContent().getDataFileId());
        switch (wrapper.getContent().getStorageAction()) {
            case STORE:
                dataStorageService.handleStoreAction(wrapper.getContent().getType(), wrapper.getContent());
                break;
            case DELETION:
                dataStorageService.handleDeletionAction(wrapper.getContent().getType(), wrapper.getContent());
                break;
            case RESTORATION:
                dataStorageService.handleRestorationAction(wrapper.getContent().getType(), wrapper.getContent());
                break;
            default:
                throw new EnumConstantNotPresentException(StorageAction.class,
                        wrapper.getContent().getStorageAction().toString());
        }
        runtimeTenantResolver.clearTenant();
    }
}
