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

import fr.cnes.regards.modules.storagelight.domain.event.FileRequestsGroupEvent.ErrorFile;

/**
 * @author sbinda
 *
 */
public interface IStorageRequestListener {

    void onCopySuccess(RequestInfo request);

    void onCopyError(RequestInfo request, Collection<ErrorFile> errors);

    void onAvailable(RequestInfo request);

    void onAvailabilityError(RequestInfo request, Collection<ErrorFile> errors);

    void onDeletionSuccess(RequestInfo request);

    void onDeletionError(RequestInfo request, Collection<ErrorFile> errors);

    void onReferenceSuccess(RequestInfo request);

    void onReferenceError(RequestInfo request, Collection<ErrorFile> errors);

    void onRequestGranted(RequestInfo request);

    void onRequestDenied(RequestInfo request);

    void onStoreSuccess(RequestInfo requestInfo);

    void onStoreError(RequestInfo requestInfo, Collection<ErrorFile> errors);

}
