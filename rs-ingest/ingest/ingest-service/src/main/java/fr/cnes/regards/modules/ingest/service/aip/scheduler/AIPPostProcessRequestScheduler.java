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

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.IngestPostProcessingJob;

/**
 * Service to handle {@link IngestPostProcessingJob}. Create {@link AIPPostProcessRequest} to post-process aips.
 * @author Iliana Ghazali
 */
@Service
@MultitenantTransactional
public class AIPPostProcessRequestScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPDeletionRequestScheduler.class);

    IAIPPostProcessRequestRepository aipPostProcessRequestRepository;

    private IAbstractRequestRepository abstractRequestRepository;

    private IAuthenticationResolver authResolver;

    private JobInfoService jobInfoService;

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    private Integer aipRequestIterationLimit;

    public AIPPostProcessRequestScheduler(IAIPPostProcessRequestRepository aipPostProcessRequestRepository,
            IAbstractRequestRepository abstractRequestRepository, IAuthenticationResolver authResolver, JobInfoService jobInfoService,
            @Value("${regards.ingest.aips.postprocess.scan.iteration-limit:100}") Integer aipRequestIterationLimit) {
        this.aipPostProcessRequestRepository = aipPostProcessRequestRepository;
        this.abstractRequestRepository = abstractRequestRepository;
        this.authResolver = authResolver;
        this.jobInfoService = jobInfoService;
        this.aipRequestIterationLimit = aipRequestIterationLimit;
    }

    /**
     * Schedule a {@link IngestPostProcessingJob} for the given {@link IngestProcessingChain} to post process given {@link AIPEntity}s
     */
    public JobInfo scheduleJob() {
        JobInfo jobInfo = null;
        LOGGER.trace("[AIP POSTPROCESS SCHEDULER] Scheduling job ...");
        long start = System.currentTimeMillis();
        // Limit the number of request
        Pageable pageRequest = PageRequest.of(0, aipRequestIterationLimit, Sort.Direction.ASC, "id");
        // Fetch the first list of AIPPostProcessRequest to handle
        Page<AIPPostProcessRequest> waitingRequest = aipPostProcessRequestRepository.findWaitingRequest(pageRequest);

        if (!waitingRequest.isEmpty()) {
            // Make a list of content ids
            List<Long> requestIds = waitingRequest.getContent().stream().map(AIPPostProcessRequest::getId)
                    .collect(Collectors.toList());
            // Change these requests state
            abstractRequestRepository.updateStates(requestIds, InternalRequestState.RUNNING);

            // Schedule aipPostProcessing jobs
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(IngestPostProcessingJob.AIP_POST_PROCESS_REQUEST_IDS, requestIds));
            jobInfo = new JobInfo(false, IngestJobPriority.POST_PROCESSING_JOB, jobParameters,
                    authResolver.getUser(), IngestPostProcessingJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);
            LOGGER.debug("[AIP POSTPROCESS SCHEDULER] 1 Job scheduled for {} AIPPostProcessRequest(s) in {} ms",
                         waitingRequest.getNumberOfElements(), System.currentTimeMillis() - start);
        }
        return jobInfo;

    }

}
