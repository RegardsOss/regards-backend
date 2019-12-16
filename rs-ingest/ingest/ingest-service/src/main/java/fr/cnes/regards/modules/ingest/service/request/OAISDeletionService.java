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
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.mapper.IOAISDeletionPayloadMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.OAISDeletionPayloadDto;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service to handle {@link OAISDeletionCreatorRequest}s
 *
 * @author Sébastien Binda
 *
 */
@Service
@MultitenantTransactional
public class OAISDeletionService implements IOAISDeletionService {

    @Autowired
    private IOAISDeletionCreatorRepository creatorRepository;

    @Autowired
    private IOAISDeletionRequestRepository requestRepository;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IOAISDeletionPayloadMapper deletionRequestMapper;

    @Override
    public Optional<OAISDeletionCreatorRequest> searchCreator(Long requestId) {
        return creatorRepository.findById(requestId);
    }

    @Override
    public List<OAISDeletionRequest> searchRequests(List<Long> deleteRequestIds) {
        return requestRepository.findAllById(deleteRequestIds);
    }

    @Override
    public void update(OAISDeletionRequest request) {
        requestRepository.save(request);
    }

    @Override
    public void registerOAISDeletionCreator(OAISDeletionPayloadDto request) {
        OAISDeletionCreatorPayload deletionPayload = deletionRequestMapper.dtoToEntity(request);
        OAISDeletionCreatorRequest deletionRequest = OAISDeletionCreatorRequest.build(deletionPayload);
        deletionRequest = (OAISDeletionCreatorRequest) requestService.scheduleRequest(deletionRequest);
        if (deletionRequest.getState() != InternalRequestState.BLOCKED) {
            requestService.scheduleJob(deletionRequest);
        }
    }

    @Override
    public void handleRemoteDeleteError(Set<RequestInfo> requestInfos) {
        // Do not handle storage deletion errors in ingest process. Storage errors will be handled in storage process.
        handleRemoteDeleteSuccess(requestInfos);
    }

    @Override
    public void handleRemoteDeleteSuccess(Set<RequestInfo> requestInfos) {
        List<AbstractRequest> requests = requestService.findRequestsByGroupIdIn(requestInfos.stream()
                .map(RequestInfo::getGroupId).collect(Collectors.toList()));
        for (RequestInfo ri : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    // Storage knows files are deleted
                    // Put back request as CREATED
                    OAISDeletionRequest deletionRequest = (OAISDeletionRequest) request;
                    deletionRequest.setRequestFilesDeleted();
                    deletionRequest.clearRemoteStepGroupIds();
                    requestService.scheduleRequest(deletionRequest);
                }
            }
        }
    }
}
