/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.service.job.AIPUpdateRunnerJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.request.AIPUpdateRequestService;

/**
 * Service to handle {@link AIPUpdateRunnerJob}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class AIPUpdateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPUpdateService.class);

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private AIPUpdateRequestService aipUpdateRequestService;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private JobInfoService jobInfoService;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.scan.iteration-limit:100}")
    private Integer updateRequestIterationLimit;

    public JobInfo scheduleJob() {
        JobInfo jobInfo = null;
        LOGGER.trace("[OAIS UPDATE SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        Pageable pageRequest = PageRequest.of(0, updateRequestIterationLimit, Sort.Direction.ASC, "id");
        // Fetch the first list of update request to handle
        Page<AIPUpdateRequest> waitingRequests = aipUpdateRequestRepository.findWaitingRequest(pageRequest);
        if (!waitingRequests.isEmpty()) {
            // Fetch all update request linked to same aips
            List<AIPUpdateRequest> contents = waitingRequests.getContent();
            List<AIPUpdateRequest> requests = new ArrayList<>(contents);
            List<Long> aipIds = contents.stream().map(wr -> wr.getAip().getId()).collect(Collectors.toList());
            List<AIPUpdateRequest> linkedTasks = aipUpdateRequestRepository.findAllByAipIdIn(aipIds);
            requests.addAll(linkedTasks);
            aipUpdateRequestService.updateState(requests, InternalRequestState.RUNNING);

            // Make a list of content ids
            List<Long> requestIds = requests.stream().map(AIPUpdateRequest::getId).collect(Collectors.toList());

            // Change request state
            abstractRequestRepository.updateStates(requestIds, InternalRequestState.RUNNING);

            // Schedule deletion job
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(AIPUpdateRunnerJob.UPDATE_REQUEST_IDS, requestIds));
            jobInfo = new JobInfo(false, IngestJobPriority.UPDATE_AIP_RUNNER_PRIORITY.getPriority(), jobParameters,
                    null, AIPUpdateRunnerJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);
            LOGGER.debug("[OAIS UPDATE SCHEDULER] 1 Job scheduled for {} AIPUpdateRequest(s) in {} ms",
                         waitingRequests.getNumberOfElements(), System.currentTimeMillis() - start);
        }
        return jobInfo;
    }

}
