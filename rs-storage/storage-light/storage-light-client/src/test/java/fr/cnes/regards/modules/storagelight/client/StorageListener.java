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
        this.success.putAll(request, success);
    }

    @Override
    public void onCopyError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        this.errors.putAll(request, errors);
        this.success.putAll(request, success);

    }

    @Override
    public void onAvailable(RequestInfo request, Collection<GroupRequestsInfo> success) {
        this.success.putAll(request, success);
    }

    @Override
    public void onAvailabilityError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        this.errors.putAll(request, errors);

    }

    @Override
    public void onDeletionSuccess(RequestInfo request, Collection<GroupRequestsInfo> success) {
        this.success.putAll(request, success);
    }

    @Override
    public void onDeletionError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        this.errors.putAll(request, errors);
    }

    @Override
    public void onReferenceSuccess(RequestInfo request, Collection<GroupRequestsInfo> success) {
        this.success.putAll(request, success);
    }

    @Override
    public void onReferenceError(RequestInfo request, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        this.errors.putAll(request, errors);
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
        this.success.putAll(request, success);
    }

    @Override
    public void onStoreError(RequestInfo requestInfo, Collection<GroupRequestsInfo> success,
            Collection<GroupRequestsInfo> errors) {
        this.errors.putAll(requestInfo, errors);
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
