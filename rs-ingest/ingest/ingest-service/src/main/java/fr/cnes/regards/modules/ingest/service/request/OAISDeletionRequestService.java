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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.mapper.IOAISDeletionPayloadMapper;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import java.util.Optional;
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
    private IRequestService requestService;

    @Autowired
    private IOAISDeletionPayloadMapper deletionRequestMapper;

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
            requestService.scheduleJob(deletionRequest);
        }
    }
}
