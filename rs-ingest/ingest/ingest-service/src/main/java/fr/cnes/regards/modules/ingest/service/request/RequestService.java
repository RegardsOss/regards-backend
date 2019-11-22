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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.mapper.IRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class RequestService implements IRequestService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestService.class);

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IAIPStoreMetaDataRequestService aipSaveMetaDataService;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IRequestMapper requestMapper;

    @Override
    public void handleRemoteRequestDenied(Set<RequestInfo> requestInfos) {
        ingestRequestService.handleRemoteRequestDenied(requestInfos);
    }

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
    public Page<RequestDto> searchRequests(SearchRequestsParameters filters, Pageable pageable) throws ModuleException {
        List<RequestDto> dtoList = new ArrayList<>();
        Page<AbstractRequest> requests = abstractRequestRepository
                .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
        // Transform AbstractRequests to DTO
        for (AbstractRequest request : requests) {
            dtoList.add(requestMapper.metadataToDto(request));
        }
        return new PageImpl<>(dtoList, pageable, requests.getTotalElements());
    }
}
