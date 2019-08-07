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
package fr.cnes.regards.modules.storagelight.client;

import java.util.Collection;

import fr.cnes.regards.modules.storagelight.domain.dto.FileDeletionRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileReferenceRequestDTO;
import fr.cnes.regards.modules.storagelight.domain.dto.FileStorageRequestDTO;

/**
 * @author sbinda
 *
 */
public interface IStorageListener {

    void onAvailable(RequestInfo request);

    void onAvailabilityError(RequestInfo request, String checksum);

    void onDeletionSuccess(RequestInfo request);

    void onDeletionError(RequestInfo request, Collection<FileDeletionRequestDTO> errors);

    void onReferenceSuccess(RequestInfo request);

    void onReferenceError(RequestInfo request, Collection<FileReferenceRequestDTO> errors);

    void onRequestGranted(RequestInfo request);

    void onRequestDenied(RequestInfo request);

    /**
     * Called once per request when all files are successfully stored
     * @param requestInfo
     */
    void onStoreSuccess(RequestInfo requestInfo);

    /**
     * Called once per request when all files has been handled and at least one is in error.
     * List of error files are returned in the requestInfo.
     * @param requestInfo
     */
    void onStoreError(RequestInfo requestInfo, Collection<FileStorageRequestDTO> errors);

}
