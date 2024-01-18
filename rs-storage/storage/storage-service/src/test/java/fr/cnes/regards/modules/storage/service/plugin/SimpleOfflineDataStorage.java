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
package fr.cnes.regards.modules.storage.service.plugin;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceWithoutOwnersDto;
import fr.cnes.regards.modules.fileaccess.plugin.domain.*;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import org.apache.commons.lang.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Set;

/**
 * @author Binda sébastien
 */
@Plugin(author = "REGARDS Team",
        description = "Plugin handling the storage on local file system",
        id = "SimpleOfflineTest",
        version = "1.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleOfflineDataStorage implements IOnlineStorageLocation {

    @PluginInit
    public void init() throws IOException {
    }

    @Override
    public PreparationResponse<FileStorageWorkingSubset, FileStorageRequestAggregationDto> prepareForStorage(Collection<FileStorageRequestAggregationDto> fileReferenceRequests) {
        throw new NotImplementedException();
    }

    @Override
    public void store(FileStorageWorkingSubset workingSubset, IStorageProgressManager progressManager) {
        throw new NotImplementedException();
    }

    @Override
    public PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequestDto> prepareForDeletion(Collection<FileDeletionRequestDto> fileDeletionRequests) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(FileDeletionWorkingSubset workingSubset, IDeletionProgressManager progressManager) {
        throw new NotImplementedException();
    }

    @Override
    public InputStream retrieve(FileReferenceWithoutOwnersDto fileRef) throws ModuleException {
        throw new NotImplementedException();
    }

    @Override
    public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequestDto> prepareForRestoration(Collection<FileCacheRequestDto> requests) {
        throw new NotImplementedException();
    }

    @Override
    public boolean allowPhysicalDeletion() {
        return false;
    }

    @Override
    public boolean isValidUrl(String urlToValidate, Set<String> errors) {
        errors.add("Simulated url validation error for tests");
        return false;
    }

}
