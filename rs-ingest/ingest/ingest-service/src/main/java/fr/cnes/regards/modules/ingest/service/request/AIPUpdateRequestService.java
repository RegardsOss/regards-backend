/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service to handle {@link AIPUpdateRequest} entities.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class AIPUpdateRequestService {

    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    private IAbstractRequestRepository abstractRequestRepository;

    private IRequestService requestService;

    private ISIPRepository sipRepository;

    public AIPUpdateRequestService(IAIPUpdateRequestRepository aipUpdateRequestRepository,
                                   IAbstractRequestRepository abstractRequestRepository,
                                   IRequestService requestService,
                                   ISIPRepository sipRepository) {
        this.aipUpdateRequestRepository = aipUpdateRequestRepository;
        this.abstractRequestRepository = abstractRequestRepository;
        this.requestService = requestService;
        this.sipRepository = sipRepository;
    }

    /**
     * Creates new {@link AIPUpdateRequest}s for the given {@link AIPEntity}s and each given {@link AbstractAIPUpdateTask}
     *
     * @param aips        {@link AIPEntity}s to update
     * @param updateTasks {@link AbstractAIPUpdateTask}s update tasks
     */
    public int create(Collection<AIPEntity> aips, Collection<AbstractAIPUpdateTask> updateTasks) {
        int nbScheduled = 0;
        if (!aips.isEmpty()) {
            List<Long> aipIds = aips.stream().map(wr -> wr.getId()).collect(Collectors.toList());
            List<AIPUpdateRequest> runningRequests = aipUpdateRequestRepository.findRunningRequestAndAipIdIn(aipIds);
            List<AbstractRequest> requests = createRequests(aips, updateTasks, runningRequests);
            if (!requests.isEmpty()) {
                nbScheduled = requestService.scheduleRequests(requests);
            }
        }
        return nbScheduled;
    }

    /**
     * Create {@link AbstractAIPUpdateTask} tasks for the given {@link AIPEntity}.
     *
     * @param aipTasks Map key : {@link AIPEntity} to update, value : {@link AbstractAIPUpdateTask}s to apply for update
     */
    public int create(Multimap<AIPEntity, AbstractAIPUpdateTask> aipTasks) {
        int nbScheduled = 0;
        List<AbstractRequest> requests = new ArrayList<>();
        if (!aipTasks.isEmpty()) {
            List<Long> aipIds = aipTasks.keySet().stream().map(wr -> wr.getId()).collect(Collectors.toList());
            List<AIPUpdateRequest> runningRequests = aipUpdateRequestRepository.findRunningRequestAndAipIdIn(aipIds);
            aipTasks.asMap().forEach((aipEntity, tasks) -> {
                requests.addAll(createRequests(Lists.newArrayList(aipEntity), tasks, runningRequests));
            });
            nbScheduled = requestService.scheduleRequests(requests);
        }
        return nbScheduled;
    }

    /**
     * Generates  {@link AbstractRequest}s from list of {@link AIPEntity}s with same list of {@link AbstractAIPUpdateTask}s
     *
     * @return {@link AbstractRequest}s created
     */
    private List<AbstractRequest> createRequests(Collection<AIPEntity> aips,
                                                 Collection<AbstractAIPUpdateTask> updateTasks,
                                                 List<AIPUpdateRequest> runningRequests) {
        List<AbstractRequest> requests = new ArrayList<>();
        // Test if there is some AIPs referenced by some running requests
        // Create the list of AIP id (and not aipId!)
        List<Long> runningAIPIds = runningRequests.stream().map(wr -> wr.getAip().getId()).collect(Collectors.toList());
        for (AIPEntity aip : aips) {
            // Create the request as BLOCKED if there is already a running request
            boolean isPending = runningAIPIds.contains(aip.getId());
            List<AIPUpdateRequest> generatedRequests = AIPUpdateRequest.build(aip, updateTasks, isPending);
            for (AIPUpdateRequest request : generatedRequests) {
                requests.add(request);
            }
        }
        return requests;
    }

    /**
     * Search for {@link AIPUpdateRequest}s by request state
     *
     * @return matching {@link AIPUpdateRequest}s
     */
    @Transactional(readOnly = true)
    public Page<AIPUpdateRequest> search(InternalRequestState requestState, Pageable page) {
        return aipUpdateRequestRepository.findAllByState(requestState, page);
    }

    /**
     * Allow to update state of given {@link AIPUpdateRequest}s
     *
     * @param requests {@link AIPUpdateRequest}s to update
     * @param state    new {@link InternalRequestState} to set for each request
     */
    public void updateState(Collection<AIPUpdateRequest> requests, InternalRequestState state) {
        if ((requests != null) && !requests.isEmpty()) {
            abstractRequestRepository.updateStates(Lists.newArrayList(requests.stream()
                                                                              .map(AIPUpdateRequest::getId)
                                                                              .collect(Collectors.toList())), state);
        }
    }
}
