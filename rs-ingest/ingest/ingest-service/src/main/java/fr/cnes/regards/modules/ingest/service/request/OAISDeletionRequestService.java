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

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.mapper.IOAISDeletionPayloadMapper;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.ingest.service.job.IngestJobPriority;
import fr.cnes.regards.modules.ingest.service.job.OAISEntityDeletionJob;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to handle {@link OAISDeletionRequest}s
 *
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class OAISDeletionRequestService {

    @Autowired
    private IOAISDeletionRequestRepository repository;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IOAISDeletionPayloadMapper deletionRequestMapper;

    /**
     * Delete a {@link OAISDeletionRequest}
     * @param request
     */
    public void deleteRequest(OAISDeletionRequest request) {
        if ((request.getJobInfo() != null) && !request.getJobInfo().isLocked()) {
            JobInfo jobInfoToUnlock = request.getJobInfo();
            jobInfoToUnlock.setLocked(false);
            jobInfoService.save(jobInfoToUnlock);
        }
        repository.delete(request);
    }

    /**
     * Search a {@link OAISDeletionRequest} by is id.
     * @param requestId
     * @return {@link OAISDeletionRequest}
     */
    public Optional<OAISDeletionRequest> search(Long requestId) {
        return repository.findById(requestId);
    }


    /**
     * Register deletion request from flow item
     * @param request to register as deletion request
     */
    public void registerOAISDeletionRequest(OAISDeletionPayloadDto request) {
        OAISDeletionPayload deletionPayload = deletionRequestMapper.dtoToEntity(request);
        OAISDeletionRequest deletionRequest = OAISDeletionRequest.build(deletionPayload);
        scheduleDeletionJob(deletionRequest);
    }

    /**
     * Try to schedule the deletion job
     * @param deletionRequest
     */
    public void scheduleDeletionJob(OAISDeletionRequest deletionRequest) {
        deletionRequest = (OAISDeletionRequest) requestService.scheduleRequest(deletionRequest);
        if (deletionRequest.getState() != InternalRequestState.BLOCKED) {
            deletionRequest.setState(InternalRequestState.RUNNING);
            // Schedule deletion job
            Set<JobParameter> jobParameters = Sets.newHashSet();
            jobParameters.add(new JobParameter(OAISEntityDeletionJob.ID, deletionRequest.getId()));
            JobInfo jobInfo = new JobInfo(false, IngestJobPriority.SESSION_DELETION_JOB_PRIORITY.getPriority(),
                    jobParameters, authResolver.getUser(), OAISEntityDeletionJob.class.getName());
            // Lock job to avoid automatic deletion. The job must be unlock when the link to the request is removed.
            jobInfo.setLocked(true);
            jobInfoService.createAsQueued(jobInfo);
            deletionRequest.setJobInfo(jobInfo);

            // save request (same transaction)
            updateRequest(deletionRequest);
        }
    }

    /**
     * Update a {@link OAISDeletionRequest}
     * @param request
     * @return
     */
    private OAISDeletionRequest updateRequest(OAISDeletionRequest request) {
        // Before saving entity check the state of the associated job if any
        if ((request.getJobInfo() != null) && !request.getJobInfo().isLocked()) {
            // Lock the job info before saving entity in order to avoid deletion of this job by an other process
            JobInfo jobInfo = request.getJobInfo();
            jobInfo.setLocked(true);
            jobInfoService.save(jobInfo);
            request.setJobInfo(jobInfo);
        }
        return repository.save(request);
    }
}
