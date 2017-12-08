/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.store;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * Handler to update AIPEntity state when a AIPEvent is received from archiva storage.
 * @author Sébastien Binda
 */
@Component
public class AIPEventHandler implements IHandler<AIPEvent>, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPEventHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IAipEntityClient aipEntityClient;

    /**
     * {@link ISubscriber} instance
     */
    @Autowired
    private ISubscriber subscriber;

    /**
     * Subscribe to DataStorageEvent in order to update AIPs state for each successfully stored AIP.
     */
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AIPEvent.class, this);
    }

    @Override
    public void handle(TenantWrapper<AIPEvent> pWrapper) {
        AIPEvent event = pWrapper.getContent();
        if ((event != null) && (event.getAipState() != null)) {
            LOGGER.debug("AIP Event received : {} - {} - {}", pWrapper.getTenant(), event.getIpId(),
                         event.getAipState());
            switch (event.getAipState()) {
                case STORAGE_ERROR:
                    handleStorageError(pWrapper.getTenant(), event.getIpId(), event.getFailureCause());
                    break;
                case STORED:
                    handleStorageSuccess(pWrapper.getTenant(), event.getIpId());
                    break;
                case DELETED:
                    handleDeleted(pWrapper.getTenant(), event.getIpId(), event.getSipId());
                    break;
                case PENDING:
                case STORING_METADATA:
                case UPDATED:
                case VALID:
                default:
                    break;
            }
        }
    }

    private void handleStorageError(String tenant, String ipId, String failureCause) {
        // AIP Unsuccessfully stored
        runtimeTenantResolver.forceTenant(tenant);
        // Retrieve aip and set the new status to stored
        aipService.setAipInError(ipId, AIPState.STORE_ERROR, failureCause);
        runtimeTenantResolver.clearTenant();
    }

    private void handleDeleted(String tenant, String ipId, String sipIpId) {
        // AIP Deleted
        runtimeTenantResolver.forceTenant(tenant);
        aipService.deleteAip(ipId, sipIpId);
        runtimeTenantResolver.clearTenant();
    }

    private void handleStorageSuccess(String tenant, String ipId) {
        // AIP Successfully stored
        runtimeTenantResolver.forceTenant(tenant);
        aipService.setAipToStored(ipId);
        runtimeTenantResolver.clearTenant();
    }

}
