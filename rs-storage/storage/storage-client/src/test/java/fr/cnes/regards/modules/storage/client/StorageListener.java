/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.client;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * @author sbinda
 */
@Component
public class StorageListener implements IStorageRequestListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(StorageListener.class);

    private final Set<RequestInfo> denied = Sets.newHashSet();

    private final Set<RequestInfo> granted = Sets.newHashSet();

    private final ArrayListMultimap<RequestInfo, RequestResultInfoDTO> success = ArrayListMultimap.create();

    private final ArrayListMultimap<RequestInfo, RequestResultInfoDTO> errors = ArrayListMultimap.create();

    public void reset() {
        denied.clear();
        granted.clear();
        success.clear();
        errors.clear();
    }

    public int getNbRequestEnds() {
        return success.asMap().keySet().size() + errors.asMap().keySet().size();
    }

    @Override
    public void onCopySuccess(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Copy success for group {} with {} success",
                         ri.getGroupId(),
                         ri.getSuccessRequests().size());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onCopyError(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Copy error for group {} with {} errors and {} success",
                         ri.getGroupId(),
                         errors.size(),
                         success.size());
            this.errors.putAll(ri, ri.getErrorRequests());
            this.success.putAll(ri, ri.getSuccessRequests());
        }

    }

    @Override
    public void onAvailable(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Availability success for group {} with {} success",
                         ri.getGroupId(),
                         success.size());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onAvailabilityError(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Availability error for group {} with {} errors and {} success",
                         ri.getGroupId(),
                         errors.size(),
                         success.size());
            this.errors.putAll(ri, ri.getErrorRequests());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onDeletionSuccess(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Deletion success for group {} with {} success",
                         ri.getGroupId(),
                         success.size());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onDeletionError(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Deletion error for group {} with {} errors and {} success",
                         ri.getGroupId(),
                         errors.size(),
                         success.size());
            this.errors.putAll(ri, ri.getErrorRequests());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onReferenceSuccess(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Reference success for group {} with {} success",
                         ri.getGroupId(),
                         success.size());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onReferenceError(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Reference error for group {} with {} errors and {} success",
                         ri.getGroupId(),
                         errors.size(),
                         success.size());
            this.errors.putAll(ri, ri.getErrorRequests());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onRequestGranted(Set<RequestInfo> requests) {
        granted.addAll(requests);

    }

    @Override
    public void onRequestDenied(Set<RequestInfo> requests) {
        denied.addAll(requests);
    }

    @Override
    public void onStoreSuccess(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Storage success for group {} with {} success",
                         ri.getGroupId(),
                         ri.getSuccessRequests().size());
            for (RequestResultInfoDTO r : ri.getSuccessRequests()) {
                LOGGER.trace("-> {}", r.getResultFile().getMetaInfo().getFileName());
            }
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    @Override
    public void onStoreError(Set<RequestInfo> requests) {
        for (RequestInfo ri : requests) {
            LOGGER.debug("[TEST RESULT] - Storage error for group {} with {} errors and {} success",
                         ri.getGroupId(),
                         errors.size(),
                         success.size());
            this.errors.putAll(ri, ri.getErrorRequests());
            this.success.putAll(ri, ri.getSuccessRequests());
        }
    }

    public Set<RequestInfo> getDenied() {
        return denied;
    }

    public Set<RequestInfo> getGranted() {
        return granted;
    }

    public ArrayListMultimap<RequestInfo, RequestResultInfoDTO> getSuccess() {
        return success;
    }

    public ArrayListMultimap<RequestInfo, RequestResultInfoDTO> getErrors() {
        return errors;
    }

}
