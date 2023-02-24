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

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/**
 * Plugin interface for all storage systems.
 *
 * @author Sébastien Binda
 */
@PluginInterface(description = "Contract to respect by any data storage plugin")
public interface IStorageLocation {

    Logger LOG = LoggerFactory.getLogger(IStorageLocation.class);

    /**
     * Dispatch given storage requests in one or many working subsets. Each subset will result to a storage job.
     *
     * @param fileReferenceRequests {@link FileStorageRequest}s to dispatch
     * @return generated subsets.
     */
    PreparationResponse<FileStorageWorkingSubset, FileStorageRequest> prepareForStorage(Collection<FileStorageRequest> fileReferenceRequests);

    /**
     * Dispatch given deletion requests in one or many working subsets. Each subset will result to a deletion job.
     *
     * @param fileDeletionRequests {@link FileDeletionRequest}s to dispatch
     * @return generated subsets.
     */
    PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequest> prepareForDeletion(Collection<FileDeletionRequest> fileDeletionRequests);

    /**
     * Dispatch given cache requests in one or many working subsets. Each subset will result to a restoration job.
     *
     * @param requests {@link FileCacheRequest}s to dispatch
     * @return generated subsets.
     */
    PreparationResponse<FileRestorationWorkingSubset, FileCacheRequest> prepareForRestoration(Collection<FileCacheRequest> requests);

    /**
     * Delete files included in the given working subset. Subset has been prepared by {@link #prepareForDeletion(Collection)}.
     * {@link IDeletionProgressManager} is used to inform process of files deletion success or error.
     */
    void delete(FileDeletionWorkingSubset workingSet, IDeletionProgressManager progressManager);

    /**
     * Store files included in the given working subset. Subset has been prepared by {@link #prepareForStorage(Collection)}.
     * {@link IStorageProgressManager} is used to inform process of files storage success or error.
     */
    void store(FileStorageWorkingSubset workingSet, IStorageProgressManager progressManager);

    default void runPeriodicAction(IPeriodicActionProgressManager progressManager) {
        LOG.debug("No periodic action defined for {} storage location", this.getClass().getName());
    }

    /**
     * Allow service to validate that a file referenced on this storage location is valid.
     */
    boolean isValidUrl(String urlToValidate, Set<String> errors);

    /**
     * Does the current storage location allow physical deletion of files ?
     *
     * @return boolean
     */
    boolean allowPhysicalDeletion();

    /**
     * Retrieve storage location root path if any
     *
     * @return Optional<Path>
     */
    default Optional<Path> getRootPath() {
        return Optional.empty();
    }

}
