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
import java.util.Optional;
import java.util.Set;

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
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdatesCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IStorageDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.mapper.IRequestMapper;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.service.aip.IAIPSaveMetaDataService;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;

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
    private IAIPSaveMetaDataService aipSaveMetaDataService;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    @Autowired
    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    private IStorageDeletionRequestRepository storageDeletionRequestRepository;

    @Autowired
    private IAIPStoreMetaDataRepository aipStoreMetaDataRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IRequestMapper requestMapper;

    @Override
    public void handleRemoteRequestDenied(Set<RequestInfo> requestInfos) {
        ingestRequestService.handleRemoteRequestDenied(requestInfos);
    }

    @Override
    public void handleRemoteStoreError(Set<RequestInfo> requestInfos) {
        ingestRequestService.handleRemoteStoreError(requestInfos);
        aipSaveMetaDataService.handleManifestSaveError(requestInfos);
    }

    @Override
    public void handleRemoteStoreSuccess(Set<RequestInfo> requestInfos) {
        for (RequestInfo ri : requestInfos) {
            List<AbstractRequest> requests = findRequestsByGroupId(ri.getGroupId());
            for (AbstractRequest request : requests) {
                if (request instanceof IngestRequest) {
                    // Retrieve request
                    Optional<IngestRequest> requestOp = ingestRequestRepository.findOneWithAIPs(ri.getGroupId());

                    if (requestOp.isPresent()) {
                        ingestRequestService.handleRemoteStoreSuccess(requestInfos);
                    }
                } else if (request instanceof AIPStoreMetaDataRequest) {
                    aipSaveMetaDataService.handleManifestSaved((AIPStoreMetaDataRequest) request, requestInfos);
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
    public List<AbstractRequest> findRequestsByGroupId(String groupId) {
        return abstractRequestRepository.findAll(AbstractRequestSpecifications.searchAllByRemoteStepGroupId(groupId));
    }

    @Override
    public Page<RequestDto> searchRequests(SearchRequestsParameters filters, Pageable pageable) throws ModuleException {
        List<RequestDto> dtoList = new ArrayList<>();
        Page<AbstractRequest> requests;
        // Use the right repository to find entities
        if (filters.getRequestType() != null) {
            switch (filters.getRequestType()) {
                case INGEST:
                    requests = ingestRequestRepository
                            .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
                    break;
                case AIP_UPDATES_CREATOR:
                    requests = aipUpdatesCreatorRepository
                            .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
                    break;
                case OAIS_DELETION:
                    requests = oaisDeletionRequestRepository
                            .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
                    break;
                case STORAGE_DELETION:
                    requests = storageDeletionRequestRepository
                            .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
                    break;
                case STORE_METADATA:
                    requests = aipStoreMetaDataRepository
                            .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
                    break;
                case UPDATE:
                    requests = aipUpdateRequestRepository
                            .findAll(AbstractRequestSpecifications.searchAllByFilters(filters), pageable);
                    break;
                default:
                    throw new ModuleException("Unexpected state received : " + filters.getRequestType());
            }
        } else {
            // Fallback to the mother's repository
            requests = abstractRequestRepository.findAll(AbstractRequestSpecifications.searchAllByFilters(filters),
                                                         pageable);
        }
        // Transform AbstractRequests to DTO
        for (AbstractRequest request : requests) {
            dtoList.add(requestMapper.metadataToDto(request));
        }
        return new PageImpl<>(dtoList, pageable, requests.getTotalElements());
    }
}
