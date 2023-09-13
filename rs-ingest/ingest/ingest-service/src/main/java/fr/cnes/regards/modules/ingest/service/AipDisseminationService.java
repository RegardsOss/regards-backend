/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.dto.request.dissemination.AIPDisseminationRequestDto;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.job.AipDisseminationCreatorJob;
import fr.cnes.regards.modules.ingest.service.job.AipDisseminationJob;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.request.RequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This services contains all methods about Aip dissemination.
 *
 * @author Thomas GUILLOU
 **/
@Service
@MultitenantTransactional
public class AipDisseminationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AipDisseminationService.class);

    /**
     * Limit number of AIPs to retrieve in one page.
     */
    @Value("${regards.ingest.aips.dissemination.bulk:1000}")
    private Integer numberOfAipMaxPerDisseminationJob;

    private final RequestService requestService;

    private final IAIPService aipService;

    private final JobInfoService jobInfoService;

    private final IAipDisseminationRequestRepository aipDisseminationRequestRepository;

    private final IAbstractRequestRepository abstractRequestRepository;

    public AipDisseminationService(RequestService requestService,
                                   IAIPService aipService,
                                   JobInfoService jobInfoService,
                                   IAipDisseminationRequestRepository aipDisseminationRequestRepository,
                                   IAbstractRequestRepository abstractRequestRepository) {
        this.requestService = requestService;
        this.aipService = aipService;
        this.jobInfoService = jobInfoService;
        this.aipDisseminationRequestRepository = aipDisseminationRequestRepository;
        this.abstractRequestRepository = abstractRequestRepository;
    }

    /**
     * Create a {@link AipDisseminationCreatorRequest} and schedule a {@link AipDisseminationCreatorJob} associated
     */
    public void registerDisseminationCreator(AIPDisseminationRequestDto disseminationRequestDto) {
        LOGGER.info("Create a dissemination request creator job for recipients {}",
                    disseminationRequestDto.recipients());
        AipDisseminationCreatorRequest creatorRequest = AipDisseminationCreatorRequest.build(disseminationRequestDto);
        requestService.scheduleRequests(List.of(creatorRequest));
        if (creatorRequest.getState() != InternalRequestState.BLOCKED) {
            requestService.scheduleJob(creatorRequest);
        }
    }

    /**
     * Create an {@link AipDisseminationJob} which will manage a maximum of n {@link AipDisseminationRequest}.
     * Other pages of AipDisseminationRequest will be manage next time this method is called (next scheduler)
     */
    public Optional<JobInfo> scheduleDisseminationJobs() {
        Pageable pageRequest = PageRequest.of(0, numberOfAipMaxPerDisseminationJob, Sort.Direction.ASC, "id");
        JobInfo jobInfo = null;

        // Fetch the first page of dissemination request to handle
        Page<AipDisseminationRequest> waitingRequest = aipDisseminationRequestRepository.findWaitingRequest(pageRequest);
        if (!waitingRequest.isEmpty()) {

            // Make a list of content ids
            List<Long> requestIds = waitingRequest.getContent()
                                                  .stream()
                                                  .map(AipDisseminationRequest::getId)
                                                  .collect(Collectors.toList());

            // Change these requests state
            abstractRequestRepository.updateStates(requestIds, InternalRequestState.RUNNING);

            // Create dissemination job
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(AipDisseminationJob.AIP_DISSEMINATION_REQUEST_IDS, requestIds));
            jobInfo = new JobInfo(false,
                                  IngestJobPriority.AIP_DISSEMINATION_JOB_PRIORITY,
                                  jobParameters,
                                  null,
                                  AipDisseminationJob.class.getName());
            jobInfoService.createAsQueued(jobInfo);

        }
        return Optional.ofNullable(jobInfo);
    }

    public List<AipDisseminationRequest> searchRequests(List<Long> aipRequestIds) {
        return aipDisseminationRequestRepository.findAllById(aipRequestIds);
    }

    public Page<AipDisseminationRequest> getAllRequests(PageRequest pageable) {
        return aipDisseminationRequestRepository.findAll(pageable);
    }
}
