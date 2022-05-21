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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.OAISDeletionJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service to handle {@link OAISDeletionJob}s
 *
 * @author Léo Mieulet
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class AIPDeletionRequestScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPDeletionRequestScheduler.class);

    private JobInfoService jobInfoService;

    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    private IAbstractRequestRepository abstractRequestRepository;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    private Integer deletionRequestIterationLimit;

    public AIPDeletionRequestScheduler(JobInfoService jobInfoService,
                                       IOAISDeletionRequestRepository oaisDeletionRequestRepository,
                                       IAbstractRequestRepository abstractRequestRepository,
                                       @Value("${regards.ingest.aips.scan.iteration-limit:100}")
                                       Integer deletionRequestIterationLimit) {
        this.jobInfoService = jobInfoService;
        this.oaisDeletionRequestRepository = oaisDeletionRequestRepository;
        this.abstractRequestRepository = abstractRequestRepository;
        this.deletionRequestIterationLimit = deletionRequestIterationLimit;
    }

    public JobInfo scheduleJob() {
        JobInfo jobInfo = null;
        LOGGER.trace("[OAIS DELETION SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        Pageable pageRequest = PageRequest.of(0, deletionRequestIterationLimit, Sort.Direction.ASC, "id");
        // Fetch the first list of update request to handle
        Page<OAISDeletionRequest> waitingRequest = oaisDeletionRequestRepository.findWaitingRequest(pageRequest);
        if (!waitingRequest.isEmpty()) {

            // Make a list of content ids
            List<Long> requestIds = waitingRequest.getContent()
                                                  .stream()
                                                  .map(OAISDeletionRequest::getId)
                                                  .collect(Collectors.toList());

            // Change these requests state
            abstractRequestRepository.updateStates(requestIds, InternalRequestState.RUNNING);

            // Schedule deletion job
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(OAISDeletionJob.OAIS_DELETION_REQUEST_IDS, requestIds));
            jobInfo = new JobInfo(false,
                                  IngestJobPriority.OAIS_DELETION_JOB_PRIORITY,
                                  jobParameters,
                                  null,
                                  OAISDeletionJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);
            LOGGER.debug("[OAIS DELETION SCHEDULER] 1 Job scheduled for {} OAISDeletionRequest(s) in {} ms",
                         waitingRequest.getNumberOfElements(),
                         System.currentTimeMillis() - start);
        }

        return jobInfo;
    }

}
