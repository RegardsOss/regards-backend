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
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPSaveMetaDataRequest;
import fr.cnes.regards.modules.ingest.service.aip.IAIPSaveMetaDataService;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private IAbstractRequestService abstractRequestService;


    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

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
            List<AbstractRequest> requests = abstractRequestService.findRequests(ri.getGroupId());
            for (AbstractRequest request : requests) {
                if (request instanceof IngestRequest) {
                    // Retrieve request
                    Optional<IngestRequest> requestOp = ingestRequestRepository.findOneWithAIPs(ri.getGroupId());

                    if (requestOp.isPresent()) {
                        IngestRequest request2 = requestOp.get();
                        ingestRequestService.handleRemoteStoreSuccess(requestInfos);
                    }
                } else if (request instanceof AIPSaveMetaDataRequest) {
                    aipSaveMetaDataService.handleManifestSaved((AIPSaveMetaDataRequest) request, requestInfos);
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
}
