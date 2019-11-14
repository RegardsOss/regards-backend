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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;

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
    private IJobInfoRepository jobInfoService;

    /**
     * Creates or updates a {@link OAISDeletionRequest}
     * @param request
     * @return
     */
    public OAISDeletionRequest saveRequest(OAISDeletionRequest request) {
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

}
