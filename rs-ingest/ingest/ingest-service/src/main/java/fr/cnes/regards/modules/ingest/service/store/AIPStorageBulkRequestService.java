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
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
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

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private ISubscriber subscriber;

    @Value("${regards.ingest.aips.bulk.request.limit:10000}")
    private Integer bulkRequestLimit;

    /**
     * Subscribe to DataStorageEvent in order to update AIPs state for each successfully stored AIP.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void onApplicationEvent(ApplicationReadyEvent pEvent) {
        subscriber.subscribeTo(AIPEvent.class, new AIPEventHandler());
    }

    @Override
    public void postAIPStorageBulkRequest() {

        // 1. Retrieve all aip ready to be stored
        Set<Long> aipIds = aipRepository.findIdByState(AIPState.CREATED);

        // 2. Use archival storage client to post the associated request
        AIPCollection aips = new AIPCollection();
        Iterator<Long> it = aipIds.iterator();
        Set<Long> aipsInRequest = Sets.newHashSet();
        while ((aipsInRequest.size() < bulkRequestLimit) && it.hasNext()) {
            Long aipId = it.next();
            aips.add(aipRepository.findOne(it.next()).getAip());
            aipsInRequest.add(aipId);
        }
        aipClient.store(aips);
        // TODO : Read rejected aips and set there status to AIP_REJECTED
        // TODO : Set accepted aips to QUEUED
        aipsInRequest.forEach(aipId -> aipRepository.updateAIPEntityState(AIPState.QUEUED, aipId));
    }

}
