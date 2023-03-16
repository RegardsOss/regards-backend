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
package fr.cnes.regards.modules.ingest.service.aip.scheduler;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.service.request.IngestRequestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to schedule IngestRequest
 *
 * @author Thibaud Michaudel
 */
@Service
@MultitenantTransactional
public class IngestRequestSchedulerService {

    private final IngestRequestService ingestRequestService;

    @Value("${regards.ingest.request.scheduler.page.size:500}")
    private int pageSize;

    public IngestRequestSchedulerService(IngestRequestService ingestRequestService) {
        this.ingestRequestService = ingestRequestService;
    }

    /**
     * Schedule the requests
     */
    //This method should not be called outside the scheduler
    public void scheduleRequests() {
        Pageable pageableIngestRequestToSchedule = PageRequest.of(0, pageSize);
        Page<IngestRequest> pageIngestRequestStep;
        do {
            pageIngestRequestStep = ingestRequestService.findToSchedule(pageableIngestRequestToSchedule);
            List<IngestRequest> requestsToSchedule = pageIngestRequestStep.toList();
            if (requestsToSchedule.isEmpty()) {
                return;
            }
            List<String> providerIds = requestsToSchedule.stream()
                                                         .map(request -> request.getProviderId())
                                                         .distinct()
                                                         .toList();
            List<IngestRequest> potentiallyBlockingRequests = ingestRequestService.findPotentiallyBlockingRequests(
                providerIds);

            List<IngestRequest> requestsReady = new ArrayList<>();
            for (IngestRequest request : requestsToSchedule) {
                if (canProceedWithRequest(request, potentiallyBlockingRequests)) {
                    requestsReady.add(request);
                } else {
                    ingestRequestService.blockRequest(request);
                }
            }
            Map<String, List<IngestRequest>> requestsReadyByChainMap = requestsReady.stream()
                                                                                    .collect(Collectors.groupingBy(
                                                                                        request -> request.getMetadata()
                                                                                                          .getIngestChain()));
            requestsReadyByChainMap.forEach(ingestRequestService::scheduleIngestProcessingJobByChain);
            pageableIngestRequestToSchedule = pageIngestRequestStep.nextPageable();
        } while (pageIngestRequestStep.hasNext());
    }

    /**
     * Check that there is no older request dealing with the same providerId being processed
     */
    private boolean canProceedWithRequest(IngestRequest requestToCheck, List<IngestRequest> requests) {
        List<IngestRequest> requestsWithSameProviderId = requests.stream()
                                                                 .filter(request -> request.getProviderId()
                                                                                           .equals(requestToCheck.getProviderId()))
                                                                 .toList();
        if (requestsWithSameProviderId.stream()
                                      .anyMatch(request -> request.getState() == InternalRequestState.CREATED
                                                           || request.getState() == InternalRequestState.RUNNING)) {
            //Another request with the same providerId is already running
            return false;
        }
        //Check that the given request is the oldest one with the state TO_SCHEDULE
        Optional<IngestRequest> oldestRequest = requestsWithSameProviderId.stream()
                                                                          .filter(request -> request.getState()
                                                                                             == InternalRequestState.TO_SCHEDULE)
                                                                          .min(Comparator.comparing(request -> request.getSubmissionDate()
                                                                                                               != null ?
                                                                              request.getSubmissionDate() :
                                                                              request.getCreationDate()));
        if (oldestRequest.isEmpty()) {
            return false;
        }
        return oldestRequest.get().getRequestId().equals(requestToCheck.getRequestId());
    }

}
