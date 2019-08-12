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

import fr.cnes.regards.modules.storagelight.domain.event.FileRequestEvent.ErrorFile;

/**
 * @author sbinda
 *
 */
@Component
public class StorageListener implements IStorageRequestListener {

    private final Set<RequestInfo> denied = Sets.newHashSet();

    private final Set<RequestInfo> granted = Sets.newHashSet();

    private final Set<RequestInfo> success = Sets.newHashSet();

    private final ArrayListMultimap<RequestInfo, ErrorFile> errors = ArrayListMultimap.create();

    public void reset() {
        denied.clear();
        granted.clear();
        success.clear();
        errors.clear();
    }

    @Override
    public void onAvailable(RequestInfo request) {
        success.add(request);
    }

    @Override
    public void onAvailabilityError(RequestInfo request, Collection<ErrorFile> errors) {
        this.errors.putAll(request, errors);

    }

    @Override
    public void onDeletionSuccess(RequestInfo request) {
        success.add(request);
    }

    @Override
    public void onDeletionError(RequestInfo request, Collection<ErrorFile> errors) {
        this.errors.putAll(request, errors);
    }

    @Override
    public void onReferenceSuccess(RequestInfo request) {
        success.add(request);
    }

    @Override
    public void onReferenceError(RequestInfo request, Collection<ErrorFile> errors) {
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
    public void onStoreSuccess(RequestInfo requestInfo) {
        success.add(requestInfo);
    }

    @Override
    public void onStoreError(RequestInfo requestInfo, Collection<ErrorFile> errors) {
        this.errors.putAll(requestInfo, errors);
    }

    public Set<RequestInfo> getDenied() {
        return denied;
    }

    public Set<RequestInfo> getGranted() {
        return granted;
    }

    public Set<RequestInfo> getSuccess() {
        return success;
    }

    public ArrayListMultimap<RequestInfo, ErrorFile> getErrors() {
        return errors;
    }

}
