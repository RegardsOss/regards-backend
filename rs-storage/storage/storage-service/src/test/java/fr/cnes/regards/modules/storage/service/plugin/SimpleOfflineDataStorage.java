/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.lang.NotImplementedException;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;
import fr.cnes.regards.modules.storage.domain.plugin.FileDeletionWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.FileRestorationWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.FileStorageWorkingSubset;
import fr.cnes.regards.modules.storage.domain.plugin.IDeletionProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineStorageLocation;
import fr.cnes.regards.modules.storage.domain.plugin.IStorageProgressManager;
import fr.cnes.regards.modules.storage.domain.plugin.PreparationResponse;

/**
 * @author Binda sébastien
 *
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the storage on local file system",
        id = "SimpleOfflineTest", version = "1.0", contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class SimpleOfflineDataStorage implements IOnlineStorageLocation {

    @PluginInit
    public void init() throws IOException {
    }

    @Override
    public PreparationResponse<FileStorageWorkingSubset, FileStorageRequest> prepareForStorage(
            Collection<FileStorageRequest> fileReferenceRequests) {
        throw new NotImplementedException();
    }

    @Override
    public void store(FileStorageWorkingSubset workingSubset, IStorageProgressManager progressManager) {
        throw new NotImplementedException();
    }

    @Override
    public PreparationResponse<FileDeletionWorkingSubset, FileDeletionRequest> prepareForDeletion(
            Collection<FileDeletionRequest> fileDeletionRequests) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(FileDeletionWorkingSubset workingSubset, IDeletionProgressManager progressManager) {
        throw new NotImplementedException();
    }

    @Override
    public InputStream retrieve(FileReference fileRef) throws ModuleException {
        throw new NotImplementedException();
    }

    @Override
    public PreparationResponse<FileRestorationWorkingSubset, FileCacheRequest> prepareForRestoration(
            Collection<FileCacheRequest> requests) {
        throw new NotImplementedException();
    }

    @Override
    public boolean allowPhysicalDeletion() {
        return false;
    }

    @Override
    public boolean isValidUrl(String urlToValidate) {
        return false;
    }

}
