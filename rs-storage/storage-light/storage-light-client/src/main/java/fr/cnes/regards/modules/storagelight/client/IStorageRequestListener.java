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

    /**
     * Callback when a group request is granted.
     * @param request
     */
    void onRequestGranted(RequestInfo request);

    /**
     * Callback when a group request is denied
     * @param request
     */
    void onRequestDenied(RequestInfo request);

    /**
     * Callback when a copy group request is successuflly done.
     * @param request
     * @param success
     */
    void onCopySuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    /**
     * Callback when a copy group request is terminated with errors.
     * @param request
     * @param success {@link GroupRequestsInfo}s successfully copied files
     * @param errors {@link GroupRequestsInfo}s copy files in error.
     */
    void onCopyError(RequestInfo request, Collection<GroupRequestsInfo> success, Collection<GroupRequestsInfo> errors);

    /**
     * Callback when a availability group request is successfully done.
     * @param request
     * @param success {@link GroupRequestsInfo}s available.
     */
    void onAvailable(RequestInfo request, Collection<GroupRequestsInfo> success);

    /**
     * Callback when a availability group request is terminated with errors.
     * @param request
     * @param success {@link GroupRequestsInfo}s available.
     * @param errors {@link GroupRequestsInfo}s not available.
     */
    void onAvailabilityError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

    /**
     * Callback when a deletion group request is successfully done.
     * @param request
     * @param success {@link GroupRequestsInfo}s deleted files
     */
    void onDeletionSuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    /** Callback when a deletion group request is terminated with errors.
     * @param request
     * @param success {@link GroupRequestsInfo}s deleted files
     * @param errors {@link GroupRequestsInfo}s not deleted files
     */
    void onDeletionError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

    /**
     * Callback when a reference group request is successfully done.
     * @param request
     * @param success {@link GroupRequestsInfo}s referenced files
     */
    void onReferenceSuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    /**
     * Callback when a reference group request is terminated with errors.
     * @param request
     * @param success {@link GroupRequestsInfo}s referenced files
     * @param errors {@link GroupRequestsInfo}s not referenced files
     */
    void onReferenceError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors);

    /**
     * Callback when a storage group request is successfully done.
     * @param requestInfo
     * @param success {@link GroupRequestsInfo}s stored files
     */
    void onStoreSuccess(RequestInfo request, Collection<GroupRequestsInfo> success);

    /**
     * Callback when a storage group request is terminated with errors.
     * @param request
     * @param success {@link GroupRequestsInfo}s stored files
     * @param errors {@link GroupRequestsInfo}s not stored files
     */
    void onStoreError(RequestInfo request, Collection<GroupRequestsInfo> success, Collection<GroupRequestsInfo> errors);

}
