/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.availability;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.filecatalog.dto.availability.NearlineFileStatusDto;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import fr.cnes.regards.modules.storage.domain.plugin.*;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Set;

/**
 * @author Thomas GUILLOU
 **/
@Plugin(author = "REGARDS Team",
        description = "Plugin handling the storage on local file system",
        id = StorageNearlineMocked.PLUGIN_ID,
        version = "1.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class StorageNearlineMocked implements INearlineStorageLocation {

    public static final String PLUGIN_ID = "NearlineT2T3Mocked";

    public static final String T3_PATTERN = "_T3";

    private int checkAvailabilityCallNumber = 0;

    public int getCheckAvailabilityCallNumber() {
        return checkAvailabilityCallNumber;
    }

    @Override
    public NearlineFileStatusDto checkAvailability(FileReference fileReference) {
        checkAvailabilityCallNumber++;
        if (fileReference.getMetaInfo().getFileName().endsWith(T3_PATTERN)) {
            return new NearlineFileStatusDto(false, null, "file is not available");
        } else {
            return new NearlineFileStatusDto(true, OffsetDateTime.now().plusHours(1), "file is available");
        }
    }

    @Override
    public void retrieve(FileRestorationWorkingSubset workingSubset, IRestorationProgressManager progressManager) {

    }

    @Override
    public PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregation> prepareForStorage(Collection<FileStorageRequestAggregation> fileReferenceRequests) {
        return null;
    }

    @Override
    public PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequest> prepareForDeletion(Collection<FileDeletionRequest> fileDeletionRequests) {
        return null;
    }

    @Override
    public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequest> prepareForRestoration(Collection<FileCacheRequest> requests) {
        return null;
    }

    @Override
    public void delete(FileDeletionWorkingSubset workingSet, IDeletionProgressManager progressManager) {

    }

    @Override
    public void store(FileStorageWorkingSubset workingSet, IStorageProgressManager progressManager) {

    }

    @Override
    public boolean isValidUrl(String urlToValidate, Set<String> errors) {
        return false;
    }

    @Override
    public boolean allowPhysicalDeletion() {
        return false;
    }
}
