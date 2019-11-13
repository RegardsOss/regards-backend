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

import java.util.Set;

import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.storagelight.client.IStorageRequestListener;
import fr.cnes.regards.modules.storagelight.client.RequestInfo;
import fr.cnes.regards.modules.storagelight.domain.dto.request.RequestResultInfoDTO;
import java.util.Collection;
import java.util.List;
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
    private IRequestService requestService;

    @Autowired
    private IDeleteRequestService deleteRequestService;

    @Override
    public void onCopySuccess(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onCopyError(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onAvailable(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onAvailabilityError(Set<RequestInfo> requests) {
        // Nothing to do
    }

    @Override
    public void onDeletionSuccess(Set<RequestInfo> requests) {
        deleteRequestService.handleRemoteDeleteSuccess(requests);
    }

    @Override
    public void onDeletionError(Set<RequestInfo> requests) {
        deleteRequestService.handleRemoteDeleteError(requests);
    }

    @Override
    public void onReferenceSuccess(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteReferenceSuccess(requests);
    }

    @Override
    public void onReferenceError(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteReferenceError(requests);
    }

    @Override
    public void onStoreSuccess(Set<RequestInfo> requests) {
        requestService.handleRemoteStoreSuccess(requests);
    }

    @Override
    public void onStoreError(Set<RequestInfo> requests) {
        requestService.handleRemoteStoreError(requests);
    }

    @Override
    public void onRequestGranted(Set<RequestInfo> requests) {
        requestService.handleRemoteRequestGranted(requests);
    }

    @Override
    public void onRequestDenied(Set<RequestInfo> requests) {
        ingestRequestService.handleRemoteRequestDenied(requests);
    }
}
