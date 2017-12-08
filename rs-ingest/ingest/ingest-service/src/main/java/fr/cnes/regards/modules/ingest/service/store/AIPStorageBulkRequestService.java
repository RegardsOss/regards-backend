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

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;
import feign.FeignException;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.RejectedAip;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * Service to send bulk request of AIP to store to archival storage microservice.
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class AIPStorageBulkRequestService
        implements IAIPStorageBulkRequestService, ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPStorageBulkRequestService.class);

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private AIPEventHandler aipEventHandler;

    @Value("${regards.ingest.aips.bulk.request.limit:10000}")
    private Integer bulkRequestLimit;

    /**
     * Subscribe to DataStorageEvent in order to update AIPs state for each successfully stored AIP.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AIPEvent.class, aipEventHandler);
    }

    @Override
    public void postAIPStorageBulkRequest() {

        // 1. Retrieve all aip ready to be stored
        Set<Long> aipIds = aipRepository.findIdByStateAndLock(AIPState.CREATED);

        // 2. Use archival storage client to post the associated request
        AIPCollection aips = new AIPCollection();
        Iterator<Long> it = aipIds.iterator();
        Set<String> aipsInRequest = Sets.newHashSet();
        while ((aipsInRequest.size() < bulkRequestLimit) && it.hasNext()) {
            Long aipId = it.next();
            AIPEntity aip = aipRepository.findOne(aipId);
            aips.add(aip.getAip());
            aipsInRequest.add(aip.getIpId());
        }
        // Update all aip in request to  AIPState to QUEUED.
        aipsInRequest.forEach(aipId -> aipRepository.updateAIPEntityState(AIPState.QUEUED, aipId));
        if (!aipsInRequest.isEmpty()) {
            FeignSecurityManager.asSystem(); // as we are using this method into a schedule, we clearly use the
            ResponseEntity<List<RejectedAip>> response = null;
            try {
                response = aipClient.store(aips);
            } catch (FeignException e) {
                // Feign only throws exceptions in case the response status is neither 404 or one of the 2xx,
                // so lets catch the exception and if it not one of our API normal status rethrow it
                if (e.status() != HttpStatus.UNPROCESSABLE_ENTITY.value()) {
                    // Response error. Microservice may be not available at the time. Update all AIPs to CREATE state to be handle next time
                    aipsInRequest.forEach(aipId -> aipRepository.updateAIPEntityState(AIPState.CREATED, aipId));
                    throw e;
                }
                //set all aip to store_rejected
                aipsInRequest.forEach(aipId -> rejectAip(aipId));
            }
            FeignSecurityManager.reset();
            if ((response != null) && (response.getStatusCode().is2xxSuccessful())) {
                List<RejectedAip> rejectedAips = response.getBody();
                // If there is rejected aips, remove them from the list of AIPEntity to set to QUEUED status.
                if ((rejectedAips != null) && !rejectedAips.isEmpty()) {
                    rejectedAips.stream().map(RejectedAip::getIpId).forEach(aipId -> rejectAip(aipId));
                }
            }
        }
    }

    private void rejectAip(String aipId) {
        LOGGER.warn("Created AIP {}, has been rejected by archival storage microservice for store action", aipId);
        aipRepository.updateAIPEntityState(AIPState.STORE_REJECTED, aipId);
    }

}
