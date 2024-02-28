/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.service.aip.IAIPDeleteService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service helper to delete requests and associated products.
 *
 * @author Sébastien Binda
 **/
@Service
@MultitenantTransactional
public class RequestDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestDeletionService.class);

    private final IAIPDeleteService aipDeleteService;

    private final ISIPService sipService;

    private final IAIPService aipService;

    private final IRequestService requestService;

    private final IIngestRequestService ingestRequestService;

    private final IPublisher publisher;

    public RequestDeletionService(IAIPDeleteService aipDeleteService,
                                  ISIPService sipService,
                                  IAIPService aipService,
                                  IRequestService requestService,
                                  IIngestRequestService ingestRequestService,
                                  IPublisher publisher) {
        this.aipDeleteService = aipDeleteService;
        this.sipService = sipService;
        this.aipService = aipService;
        this.requestService = requestService;
        this.ingestRequestService = ingestRequestService;
        this.publisher = publisher;
    }

    public void deleteRequests(Collection<? extends AbstractRequest> requests) {
        LOGGER.info("[REQUEST DELETION] Start deleting {} requests and associated AIPs/SIPs", requests.size());
        // Retrieve complete ingest requests before deletion
        List<IngestRequest> ingestRequests = ingestRequestService.findByIds(requests.stream()
                                                                                    .filter(r -> r.getDtype()
                                                                                                  .equals(
                                                                                                      RequestTypeConstant.INGEST_VALUE))
                                                                                    .map(AbstractRequest::getId)
                                                                                    .collect(Collectors.toSet()));
        // Delete requests
        LOGGER.info("[REQUEST DELETION] Start deleting {} requests.", requests.size());
        requestService.deleteRequests(requests, true);
        // Handle specific ingest request deletion
        deleteAssociatedAipAndSipForIngestRequests(ingestRequests);
    }

    /**
     * Delete associated aip and sip for given requests if request is an {@link IngestRequest}
     *
     * @param requests requests to delete AIP and SIP from.
     */
    private void deleteAssociatedAipAndSipForIngestRequests(Collection<IngestRequest> requests) {
        if (!requests.isEmpty()) {
            // If an ingest request is deleted that means that the associated AIP is not in final state and must be deleted.
            Set<AIPEntity> aipsToDelete = requests.stream()
                                                  .filter(r -> r.getAips() != null && !r.getAips().isEmpty())
                                                  .flatMap(r -> r.getAips().stream())
                                                  .collect(Collectors.toSet());
            if (!aipsToDelete.isEmpty()) {
                LOGGER.info("[REQUEST DELETION] Start deleting {} AIPs last flags.", requests.size());
                aipsToDelete.forEach(aipDeleteService::removeLastFlag);
                Set<String> sipToDelete = aipsToDelete.stream()
                                                      .map(a -> a.getSip().getSipId())
                                                      .collect(Collectors.toSet());
                LOGGER.debug("[REQUEST DELETION] Sending storage deletion requests.");
                aipDeleteService.sendLinkedFilesDeletionRequest(aipsToDelete);
                LOGGER.debug("[REQUEST DELETION] Cancel associated storage request.");
                aipDeleteService.cancelStorageRequests(requests);
                // Delete aip to delete associated sip
                LOGGER.info("[REQUEST DELETION] Start deleting all associated AIPs.");
                aipDeleteService.deleteAll(aipsToDelete);
                // Delete associated SIP if not associated to another aip
                LOGGER.info("[REQUEST DELETION] Start deleting all associated SIPs");
                List<String> sipIdsToDelete = sipToDelete.stream()
                                                         .filter(sipId -> aipService.findBySipId(sipId).isEmpty())
                                                         .toList();
                sipService.processDeletions(sipIdsToDelete, true);
                LOGGER.info("[REQUEST DELETION] Deletion of associated AIPs and SIPs done.");

                // Publish request canceled
                for (IngestRequest request : requests) {
                    publisher.publish(IngestRequestEvent.build(request.getCorrelationId(),
                                                               request.getProviderId(),
                                                               null,
                                                               RequestState.DELETED,
                                                               request.getErrors()));
                }
            }
        }
    }

}
