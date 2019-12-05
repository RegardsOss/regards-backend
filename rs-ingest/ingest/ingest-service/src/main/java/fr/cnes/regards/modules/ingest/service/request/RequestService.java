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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.mapper.IRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/**
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class RequestService implements IRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);

    @Autowired
    @Lazy
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IAIPStoreMetaDataRequestService aipSaveMetaDataService;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAIPStoreMetaDataRepository aipStoreMetaDataRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IRequestMapper requestMapper;

    @Override
    public void handleRemoteStoreError(Set<RequestInfo> requestInfos) {
        List<AbstractRequest> requests = findRequestsByGroupIdIn(requestInfos.stream().map(RequestInfo::getGroupId)
                .collect(Collectors.toList()));
        for (RequestInfo ri : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    if (request instanceof IngestRequest) {
                        ingestRequestService.handleRemoteStoreError((IngestRequest) request, ri);
                    } else if (request instanceof AIPStoreMetaDataRequest) {
                        aipSaveMetaDataService.handleError((AIPStoreMetaDataRequest) request, ri);
                    }
                }
            }
        }
    }

    @Override
    public void handleRemoteStoreSuccess(Set<RequestInfo> requestInfos) {
        List<AbstractRequest> requests = findRequestsByGroupIdIn(requestInfos.stream().map(RequestInfo::getGroupId)
                .collect(Collectors.toList()));
        for (RequestInfo ri : requestInfos) {
            for (AbstractRequest request : requests) {
                if (request.getRemoteStepGroupIds().contains(ri.getGroupId())) {
                    if (request instanceof IngestRequest) {
                        ingestRequestService.handleRemoteStoreSuccess((IngestRequest) (request), ri);
                    } else if (request instanceof AIPStoreMetaDataRequest) {
                        aipSaveMetaDataService.handleSuccess((AIPStoreMetaDataRequest) request, ri);
                    }
                }
            }
        }
    }

    @Override
    public void handleRemoteRequestGranted(Set<RequestInfo> requests) {
        // Do not track at the moment : the ongoing request could send a success too quickly
        // and could cause unnecessary concurrent access to thehandleRemoteRequestGranted database!
        for (RequestInfo ri : requests) {
            LOGGER.debug("Storage request granted with id \"{}\"", ri.getGroupId());
        }
    }

    @Override
    public List<AbstractRequest> findRequestsByGroupIdIn(List<String> groupIds) {
        return abstractRequestRepository.findAll(AbstractRequestSpecifications.searchAllByRemoteStepGroupId(groupIds));
    }

    @Override
    public Page<RequestDto> findRequests(SearchRequestsParameters filters, Pageable pageable) throws ModuleException {
        List<RequestDto> dtoList = new ArrayList<>();
        Page<AbstractRequest> requests = abstractRequestRepository
                .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
        // Transform AbstractRequests to DTO
        for (AbstractRequest request : requests) {
            dtoList.add(requestMapper.metadataToDto(request));
        }
        return new PageImpl<>(dtoList, pageable, requests.getTotalElements());
    }

    @Override
    public void deleteAllByAip(Set<AIPEntity> aipEntities) {
        for (AIPEntity aipEntity : aipEntities) {
            if (aipEntity.getState() == AIPState.GENERATED) {
                // Check there is no IngestRequest linked to this AIP
                List<IngestRequest> requests = ingestRequestRepository.findAllByAipsIn(aipEntity);
                ingestRequestRepository.deleteAll(requests);
            }
        }
        // Make the list of all these AIPs id and remove all requests associated
        List<Long> aipIds = aipEntities.stream().map(AIPEntity::getId).collect(Collectors.toList());

        List<AIPStoreMetaDataRequest> storeMetaRequests = aipStoreMetaDataRepository.findAllByAipIdIn(aipIds);
        aipStoreMetaDataRepository.deleteAll(storeMetaRequests);

        List<AIPUpdateRequest> updateRequests = aipUpdateRequestRepository.findAllByAipIdIn(aipIds);
        aipUpdateRequestRepository.deleteAll(updateRequests);
    }

    @Override
    public void scheduleRequests(List<AbstractRequest> requests) {
        // Store request state (can be scheduled right now ?) by session
        Table<String, String, InternalRequestState> stateBySession = HashBasedTable.create();

        for (AbstractRequest request : requests) {
            // Ignore BLOCKED requests
            if (request.getState() != InternalRequestState.BLOCKED) {
                String sessionOwner = request.getSessionOwner();
                String session = request.getSession();

                // Compute the session state if not already available
                if (!stateBySession.contains(sessionOwner, session)) {
                    // Check if the request can be processed right now
                    request = scheduleRequest(request);

                    // Store if request for this session can be executed right now
                    stateBySession.put(sessionOwner, session, request.getState());
                } else {
                    InternalRequestState state = stateBySession.get(sessionOwner, session);
                    request.setState(state);
                    abstractRequestRepository.save(request);
                }
            } else {
                // Just save the request as BLOCKED
                abstractRequestRepository.save(request);
            }

        }
    }

    @Override
    public AbstractRequest scheduleRequest(AbstractRequest request) {
        Specification<AbstractRequest> spec = null;
        Optional<String> sessionOwnerOp = Optional.ofNullable(request.getSessionOwner());
        Optional<String> sessionOp = Optional.ofNullable(request.getSession());
        switch (request.getDtype()) {
            case RequestTypeConstant.AIP_UPDATES_CREATOR_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingAipUpdatesCreator(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.OAIS_DELETION_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingOAISDeletion(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.STORAGE_DELETION_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingStorageDeletion(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.STORE_METADATA_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingStore(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.UPDATE_VALUE:
                spec = AbstractRequestSpecifications.searchRequestBlockingUpdate(sessionOwnerOp, sessionOp);
                break;
            case RequestTypeConstant.INGEST_VALUE:
                // Ingest cannot be blocked, don't call this method to save that kind of request
            default:
                throw new IllegalArgumentException(String.format("You should not use this method for requests having [%s] type",
                        request.getDtype()));
        }
        boolean shouldDelayCurrentRequest = abstractRequestRepository.exists(spec);
        if (shouldDelayCurrentRequest) {
            // Block the request
            request.setState(InternalRequestState.BLOCKED);
        } else if (request.getState() == InternalRequestState.TO_SCHEDULE) {
            // If the request is accepted but was in TO_SCHEDULE, put it in CREATED
            request.setState(InternalRequestState.CREATED);
        }
        // Save to repo
        return abstractRequestRepository.save(request);
    }

}
