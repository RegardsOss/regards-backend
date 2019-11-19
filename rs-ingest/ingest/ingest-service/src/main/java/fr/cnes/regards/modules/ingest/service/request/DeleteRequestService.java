/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ingest.dao.IStorageDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.StorageDeletionRequest;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import fr.cnes.regards.modules.storage.client.RequestInfo;

/**
 * Delete request service
 *
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class DeleteRequestService implements IDeleteRequestService {

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IStorageDeletionRequestRepository storageDeletionRequestRepo;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private ISIPService sipService;

    @Override
    public void handleRemoteDeleteError(Set<RequestInfo> requestInfos) {
        // Do not handle storage deletion errors in ingest process. Storage errors will be handled in storage process.
        handleRemoteDeleteSuccess(requestInfos);
    }

    @Override
    public void handleRemoteDeleteSuccess(Set<RequestInfo> requestInfos) {
        for (RequestInfo ri : requestInfos) {
            List<AbstractRequest> requests = requestService.findRequestsByGroupId(ri.getGroupId());
            for (AbstractRequest request : requests) {
                StorageDeletionRequest deletionRequest = (StorageDeletionRequest) request;
                boolean deleteIrrevocably = deletionRequest.getDeletionMode() == SessionDeletionMode.IRREVOCABLY;
                aipService.processDeletion(deletionRequest.getSipId(), deleteIrrevocably);
                sipService.processDeletion(deletionRequest.getSipId(), deleteIrrevocably);
                storageDeletionRequestRepo.delete(deletionRequest);
                // NOTE : Session as been notified in processDeletion method of aipService
            }
        }
    }
}
