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

import fr.cnes.regards.modules.storagelight.domain.database.request.group.GroupRequestsInfo;

/**
 * @author sbinda
 *
 */
public interface IStorageRequestListener {

    void onRequestGranted(RequestInfo request);

    void onRequestDenied(RequestInfo request);

    void onCopySuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    void onCopyError(RequestInfo request, Collection<GroupRequestsInfo> success, Collection<GroupRequestsInfo> errors);

    void onAvailable(RequestInfo request, Collection<GroupRequestsInfo> success);

    void onAvailabilityError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

    void onDeletionSuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    void onDeletionError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

    void onReferenceSuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    void onReferenceError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

    void onStoreSuccess(RequestInfo requestInfo, Collection<GroupRequestsInfo> success);

    void onStoreError(RequestInfo requestInfo, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

}
