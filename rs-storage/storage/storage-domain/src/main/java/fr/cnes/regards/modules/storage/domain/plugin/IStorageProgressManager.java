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
package fr.cnes.regards.modules.storage.domain.plugin;

import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;

import java.net.URL;

/**
 * The ProgressManager is used by {@link IStorageLocation} plugins to notidy the upper service of storage action results :
 * <ul>
 * <li>Storage succeed {@link #storageSucceed}</li>
 * <li>Storage failed {@link #storageFailed}</li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public interface IStorageProgressManager {

    /**
     * Notify system that the given {@link FileStorageRequestAggregation} is fully stored.
     *
     * @param fileReferenceRequest {@link FileStorageRequestAggregation} stored.
     */
    public void storageSucceed(FileStorageRequestAggregation fileReferenceRequest, URL storedUrl, Long fileSize);

    /**
     * Notify system that the given {@link FileStorageRequestAggregation} is stored, but an asynchronous action
     * is needed to fully store the file. This asynchronous action can be triggered by the plugin or
     * thanks to the {@link IStorageLocation#runPeriodicAction)}. To be fully stored after pending action
     * is over, plugin should call the {@link #storagePendingActionSucceed(String)}.
     *
     * @param fileReferenceRequest {@link FileStorageRequestAggregation} stored.
     * @param storedUrl            URL to stored file
     * @param fileSize             size of the stored file in bytes
     * @param notifyAdministrators inform administrator that an action is pending on this file
     */
    void storageSucceedWithPendingActionRemaining(FileStorageRequestAggregation fileReferenceRequest,
                                                  URL storedUrl,
                                                  Long fileSize,
                                                  Boolean notifyAdministrators);

    /**
     * Notify system that the pending action on stored file is over. So the file can be considered as
     * fully stored.
     *
     * @param storedUrl URL of the stored file on external system
     */
    void storagePendingActionSucceed(String storedUrl);

    /**
     * Notify the system that the given {@link FileStorageRequestAggregation} couldn't be stored.
     *
     * @param fileReferenceRequest {@link FileStorageRequestAggregation} not stored.
     * @param cause                {@link String} error message.
     */
    public void storageFailed(FileStorageRequestAggregation fileReferenceRequest, String cause);

}
