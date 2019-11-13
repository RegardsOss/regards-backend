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
package fr.cnes.regards.modules.ingest.service.request;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to handle {@link AIPUpdateRequest} entities.
 *
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class AIPUpdateRequestService {

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    /**
     * Creates new {@link AIPUpdateRequest}s for the given {@link AIPEntity}s and each given {@link AbstractAIPUpdateTask}
     *
     * @param aips {@link AIPEntity}s to update
     * @param updateTasks  {@link AbstractAIPUpdateTask}s update tasks
     */
    public void create(Collection<AIPEntity> aips, Collection<AbstractAIPUpdateTask> updateTasks) {
        // Test if there is some AIPs referenced by some running requests
        List<Long> aipIds = aips.stream().map(wr -> wr.getId()).collect(Collectors.toList());
        List<AIPUpdateRequest> runningRequests = aipUpdateRequestRepository.findRunningRequestAndAipIdIn(aipIds);
        // Create the list of AIP id (and not aipId!)
        List<Long> runningAIPIds = runningRequests.stream().map(wr -> wr.getAip().getId()).collect(Collectors.toList());

        for (AIPEntity aip : aips) {
            // Create the request as pending if there is already a running request
            boolean isPending = runningAIPIds.contains(aip.getId());
            List<AIPUpdateRequest> requests = AIPUpdateRequest.build(aip, updateTasks, isPending);
            aipUpdateRequestRepository.saveAll(requests);
        }
    }

    /**
     * Creates new {@link AIPUpdateRequest}s for the given {@link AIPEntity} and each given {@link AbstractAIPUpdateTask}
     *
     * @param aips {@link AIPEntity} to update
     * @param updateTasks  {@link AbstractAIPUpdateTask}s update tasks
     */
    public void create(AIPEntity aip, Collection<AbstractAIPUpdateTask> updateTasks) {
        // Test if there is some AIPs referenced by some running requests
        List<AIPUpdateRequest> runningRequests = aipUpdateRequestRepository
                .findRunningRequestAndAipIdIn(Lists.newArrayList(aip.getId()));
        // Create the list of AIP id (and not aipId!)
        List<Long> runningAIPIds = runningRequests.stream().map(wr -> wr.getAip().getId()).collect(Collectors.toList());

        // Create the request as pending if there is already a running request
        boolean isPending = runningAIPIds.contains(aip.getId());
        List<AIPUpdateRequest> requests = AIPUpdateRequest.build(aip, updateTasks, isPending);
        aipUpdateRequestRepository.saveAll(requests);
    }

    /**
     * Create {@link AbstractAIPUpdateTask} tasks for the given {@link AIPEntity}.
     *
     * @param aipTasks Map key : {@link AIPEntity} to update, value : {@link AbstractAIPUpdateTask}s to apply for update
     */
    public void create(Multimap<AIPEntity, AbstractAIPUpdateTask> aipTasks) {
        aipTasks.asMap().forEach((aipEntity, tasks) -> {
            create(aipEntity, tasks);
        });
    }

    /**
     * Search for {@link AIPUpdateRequest}s by request state
     *
     * @param requestState
     * @param page
     * @return matching {@link AIPUpdateRequest}s
     */
    @Transactional(readOnly = true)
    public Page<AIPUpdateRequest> search(InternalRequestStep requestState, Pageable page) {
        return aipUpdateRequestRepository.findAllByState(requestState, page);
    }

    /**
     * Allow to update state of given {@link AIPUpdateRequest}s
     *
     * @param requests {@link AIPUpdateRequest}s to update
     * @param state new {@link InternalRequestStep} to set for each request
     */
    public void updateState(Collection<AIPUpdateRequest> requests, InternalRequestStep state) {
        if ((requests != null) && !requests.isEmpty()) {
            abstractRequestRepository.updateStates(Lists
                    .newArrayList(requests.stream().map(AIPUpdateRequest::getId).collect(Collectors.toList())), state);
        }
    }

}
