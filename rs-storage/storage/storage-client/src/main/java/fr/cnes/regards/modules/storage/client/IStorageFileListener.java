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
package fr.cnes.regards.modules.storage.client;

import java.util.Collection;

import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.flow.AvailabilityFlowItem;
import fr.cnes.regards.modules.storage.domain.flow.DeletionFlowItem;

/**
 *
 * Listener to handle bus messages from storage service.
 *
 * @author Sébastien Binda
 *
 */
public interface IStorageFileListener {

    /**
     * Callback when a file is successfully stored or referenced
     * @param checksum Checksum of the file successfully stored or referenced
     * @param storage storage where the file is newly stored or referenced
     * @param owners Owners of the file
     */
    public void onFileStored(String checksum, String storage, Collection<String> owners,
            Collection<RequestInfo> requestInfos);

    /**
     * Callback when a file to store is in error.
     * @param checksum
     * @param storage
     * @param owners
     * @param requestInfos
     * @param errorCause
     */
    public void onFileStoreError(String checksum, String storage, Collection<String> owners,
            Collection<RequestInfo> requestInfos, String errorCause);

    /**
     * Callback when a file is available for download. Next to a {@link AvailabilityFlowItem} request.
     * @param checksum
     * @param requestInfos
     */
    public void onFileAvailable(String checksum, Collection<RequestInfo> requestInfos);

    /**
     * Callback when a file is not available for download. Next to a {@link AvailabilityFlowItem} request.
     * @param checksum
     * @param requestInfos
     */
    public void onFileNotAvailable(String checksum, Collection<RequestInfo> requestInfos, String errorCause);

    /**
     * Callback when a file is successfully deleted. Next to a {@link DeletionFlowItem} request.
     * @param checksum
     * @param storage
     * @param owner
     * @param requestInfos
     */
    public void onFileDeleted(String checksum, String storage, String owner, Collection<RequestInfo> requestInfos);

    /**
     * Callback when a file is updated.
     * @param checksum checksum of the updated file
     * @param storage storage location of the updated file
     * @param updateFile file updated.
     */
    public void onFileUpdated(String checksum, String storage, FileReference updateFile);

}
