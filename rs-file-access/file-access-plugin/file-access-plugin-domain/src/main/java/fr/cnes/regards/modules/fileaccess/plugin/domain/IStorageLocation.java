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
package fr.cnes.regards.modules.fileaccess.plugin.domain;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceWithoutOwnersDto;
import fr.cnes.regards.modules.fileaccess.dto.IStoragePluginConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.transaction.NotSupportedException;
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
     * @param fileReferenceRequests {@link FileStorageRequestAggregationDto}s to dispatch
     * @return generated subsets.
     */
    PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregationDto> prepareForStorage(Collection<FileStorageRequestAggregationDto> fileReferenceRequests);

    /**
     * Dispatch given deletion requests in one or many working subsets. Each subset will result to a deletion job.
     *
     * @param fileDeletionRequests {@link FileDeletionRequestDto}s to dispatch
     * @return generated subsets.
     */
    PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequestDto> prepareForDeletion(Collection<FileDeletionRequestDto> fileDeletionRequests);

    /**
     * Dispatch given cache requests in one or many working subsets. Each subset will result to a restoration job.
     *
     * @param requests {@link FileCacheRequestDto}s to dispatch
     * @return generated subsets.
     */
    PreparationResponse<FileRestorationWorkingSubset, FileCacheRequestDto> prepareForRestoration(Collection<FileCacheRequestDto> requests);

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

    default boolean hasPeriodicAction() {
        return false;
    }

    default void runPeriodicAction(IPeriodicActionProgressManager progressManager) {
        LOG.debug("No periodic action defined for {} storage location", this.getClass().getName());
    }

    default void runCheckPendingAction(IPeriodicActionProgressManager progressManager,
                                       Set<FileReferenceWithoutOwnersDto> filesWithPendingActions) {
        LOG.debug("No check pending action defined for {} storage location", this.getClass().getName());
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

    /**
     * Create the configuration dto to share to the workers
     *
     * @return the configuration
     */
    default IStoragePluginConfigurationDto createWorkerStoreConfiguration() throws NotSupportedException {
        throw new NotSupportedException(String.format("Worker configuration creation is not supported for %s",
                                                      this.getClass().getName()));
    }

}
