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

import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;
import fr.cnes.regards.modules.storagelight.domain.event.FileRequestType;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.modules.storagelight.client.IStorageRequestListener;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;

/**
 * This class offers callbacks from storage events
 *
 * @author Marc SORDI
 */
@Component
public class IngestStorageListener implements IStorageRequestListener {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestStorageListener.class);

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IDeleteRequestService deleteRequestService;

    @Override
    public void onCopySuccess(RequestInfo request, Collection<RequestResultInfoDTO> success) {
        // Nothing to do
    }

    @Override
    public void onCopyError(RequestInfo request, Collection<RequestResultInfoDTO> success, Collection<RequestResultInfoDTO> errors) {
        // Nothing to do
    }

    @Override
    public void onAvailable(RequestInfo request, Collection<RequestResultInfoDTO> success) {
        // Nothing to do
    }

    @Override
    public void onAvailabilityError(RequestInfo request, Collection<RequestResultInfoDTO> success, Collection<RequestResultInfoDTO> errors) {
        // Nothing to do
    }

    @Override
    public void onDeletionSuccess(RequestInfo request, Collection<RequestResultInfoDTO> success) {
        deleteRequestService.handleRemoteDeleteSuccess(request, success);
    }

    @Override
    public void onDeletionError(RequestInfo request, Collection<RequestResultInfoDTO> success, Collection<RequestResultInfoDTO> errors) {
        deleteRequestService.handleRemoteDeleteError(request, success, errors);
    }

    @Override
    public void onReferenceSuccess(RequestInfo request, Collection<RequestResultInfoDTO> success) {
        ingestRequestService.handleRemoteStoreSuccess(request, success, FileRequestType.REFERENCE);
    }

    @Override
    public void onReferenceError(RequestInfo request, Collection<RequestResultInfoDTO> success, Collection<RequestResultInfoDTO> errors) {
        ingestRequestService.handleRemoteStoreError(request, success, errors, FileRequestType.REFERENCE);
    }

    @Override
    public void onStoreSuccess(RequestInfo requestInfo, Collection<RequestResultInfoDTO> success) {
        ingestRequestService.handleRemoteStoreSuccess(requestInfo, success, FileRequestType.STORAGE);
    }

    @Override
    public void onStoreError(RequestInfo requestInfo, Collection<RequestResultInfoDTO> success, Collection<RequestResultInfoDTO> errors) {
        ingestRequestService.handleRemoteStoreError(requestInfo, success, errors, FileRequestType.STORAGE);
    }

    @Override
    public void onRequestGranted(RequestInfo requestInfo) {
        ingestRequestService.handleRemoteRequestGranted(requestInfo);
    }

    @Override
    public void onRequestDenied(RequestInfo requestInfo) {
        ingestRequestService.handleRemoteRequestDenied(requestInfo);
    }
}
