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
package fr.cnes.regards.modules.ingest.service;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
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
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.ArrayList;
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

    private final INotifierClient notifierClient;

    private final Gson gson;

    public AipDisseminationService(RequestService requestService,
                                   IAIPService aipService,
                                   JobInfoService jobInfoService,
                                   IAipDisseminationRequestRepository aipDisseminationRequestRepository,
                                   IAbstractRequestRepository abstractRequestRepository,
                                   INotifierClient notifierClient,
                                   Gson gson) {
        this.requestService = requestService;
        this.aipService = aipService;
        this.jobInfoService = jobInfoService;
        this.aipDisseminationRequestRepository = aipDisseminationRequestRepository;
        this.abstractRequestRepository = abstractRequestRepository;
        this.notifierClient = notifierClient;
        this.gson = gson;
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
    @MultitenantTransactional
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

    @MultitenantTransactional(readOnly = true)
    public List<AipDisseminationRequest> findAllById(List<Long> aipRequestIds) {
        return aipDisseminationRequestRepository.findAllById(aipRequestIds);
    }

    @MultitenantTransactional(readOnly = true)
    public Page<AipDisseminationRequest> findAll(PageRequest pageable) {
        return aipDisseminationRequestRepository.findAll(pageable);
    }

    /**
     * For {@link AipDisseminationJob}s  and {@link AipDisseminationCreatorJob}s in error, update each requests'
     * state to ERROR and add a message containing the error in each request
     */
    @MultitenantTransactional
    public boolean handleJobCrash(JobInfo jobInfo) {
        boolean isDisseminationJob = AipDisseminationJob.class.getName().equals(jobInfo.getClassName());
        boolean isDisseminationCreatorJob = AipDisseminationCreatorJob.class.getName().equals(jobInfo.getClassName());
        if (isDisseminationJob || isDisseminationCreatorJob) {
            try {
                Type type = new TypeToken<Set<Long>>() {

                }.getType();
                Set<Long> ids = IJob.getValue(jobInfo.getParametersAsMap(),
                                              isDisseminationCreatorJob ?
                                                  AipDisseminationCreatorJob.REQUEST_ID :
                                                  AipDisseminationJob.AIP_DISSEMINATION_REQUEST_IDS,
                                              type);
                List<AipDisseminationRequest> requests = findAllById(new ArrayList<>(ids));
                requests.forEach(r -> {
                    r.addError(jobInfo.getStatus().getStackTrace());
                    r.setState(InternalRequestState.ERROR);
                    aipDisseminationRequestRepository.save(r);
                });
            } catch (JobParameterMissingException | JobParameterInvalidException e) {
                LOGGER.error(String.format("Dissemination request job with id \"%s\" fails with status \"%s\"",
                                           jobInfo.getId(),
                                           jobInfo.getStatus().getStatus()));
            }
        }
        return isDisseminationJob || isDisseminationCreatorJob;
    }
}


