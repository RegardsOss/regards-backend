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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storagelight.domain.database.request.group.GroupRequestsInfo;

/**
 * @author sbinda
 *
 */
@Component
public class StorageListener implements IStorageRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageListener.class);

    private final Set<RequestInfo> denied = Sets.newHashSet();

    private final Set<RequestInfo> granted = Sets.newHashSet();

    private final ArrayListMultimap<RequestInfo, GroupRequestsInfo> success = ArrayListMultimap.create();

    private final ArrayListMultimap<RequestInfo, GroupRequestsInfo> errors = ArrayListMultimap.create();

    public void reset() {
        denied.clear();
        granted.clear();
        success.clear();
        errors.clear();
    }

    @Override
    public void onCopySuccess(RequestInfo request, Collection<GroupRequestsInfo> success) {
        LOGGER.debug("[TEST RESULT] - Copy success for group {} with {} success", request.getGroupId(), success.size());
        this.success.putAll(request, success);
    }

    @Override
    public void onCopyError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        LOGGER.debug("[TEST RESULT] - Copy error for group {} with {} errors and {} success", request.getGroupId(),
                     errors.size(), success.size());
        this.errors.putAll(request, errors);
        this.success.putAll(request, success);

    }

    @Override
    public void onAvailable(RequestInfo request, Collection<GroupRequestsInfo> success) {
        LOGGER.debug("[TEST RESULT] - Availability success for group {} with {} success", request.getGroupId(),
                     success.size());
        this.success.putAll(request, success);
    }

    @Override
    public void onAvailabilityError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        LOGGER.debug("[TEST RESULT] - Availability error for group {} with {} errors and {} success",
                     request.getGroupId(), errors.size(), success.size());
        this.errors.putAll(request, errors);
        this.success.putAll(request, success);
    }

    @Override
    public void onDeletionSuccess(RequestInfo request, Collection<GroupRequestsInfo> success) {
        LOGGER.debug("[TEST RESULT] - Deletion success for group {} with {} success", request.getGroupId(),
                     success.size());
        this.success.putAll(request, success);
    }

    @Override
    public void onDeletionError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        LOGGER.debug("[TEST RESULT] - Deletion error for group {} with {} errors and {} success", request.getGroupId(),
                     errors.size(), success.size());
        this.errors.putAll(request, errors);
        this.success.putAll(request, success);
    }

    @Override
    public void onReferenceSuccess(RequestInfo request, Collection<GroupRequestsInfo> success) {
        LOGGER.debug("[TEST RESULT] - Reference success for group {} with {} success", request.getGroupId(),
                     success.size());
        this.success.putAll(request, success);
    }

    @Override
    public void onReferenceError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        LOGGER.debug("[TEST RESULT] - Reference error for group {} with {} errors and {} success", request.getGroupId(),
                     errors.size(), success.size());
        this.errors.putAll(request, errors);
        this.success.putAll(request, success);
    }

    @Override
    public void onRequestGranted(RequestInfo request) {
        granted.add(request);

    }

    @Override
    public void onRequestDenied(RequestInfo request) {
        denied.add(request);
    }

    @Override
    public void onStoreSuccess(RequestInfo request, Collection<GroupRequestsInfo> success) {
        LOGGER.debug("[TEST RESULT] - Storage success for group {} with {} success", request.getGroupId(),
                     success.size());
        this.success.putAll(request, success);
    }

    @Override
    public void onStoreError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        LOGGER.debug("[TEST RESULT] - Storage error for group {} with {} errors and {} success", request.getGroupId(),
                     errors.size(), success.size());
        this.errors.putAll(request, errors);
        this.success.putAll(request, success);
    }

    public Set<RequestInfo> getDenied() {
        return denied;
    }

    public Set<RequestInfo> getGranted() {
        return granted;
    }

    public ArrayListMultimap<RequestInfo, GroupRequestsInfo> getSuccess() {
        return success;
    }

    public ArrayListMultimap<RequestInfo, GroupRequestsInfo> getErrors() {
        return errors;
    }

}
