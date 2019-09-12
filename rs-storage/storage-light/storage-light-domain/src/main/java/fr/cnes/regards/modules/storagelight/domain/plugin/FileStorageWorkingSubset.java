/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.plugin;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.modules.storagelight.domain.database.request.FileStorageRequest;

/**
 * Default implementation for simple file workingsubsets.
 *
 * @author Sébastien Binda
 */
public class FileStorageWorkingSubset {

    /**
     * Raw {@link FileStorageRequest}s associate
     */
    private final Set<FileStorageRequest> fileRefenreceRequests = Sets.newHashSet();

    public FileStorageWorkingSubset(Collection<FileStorageRequest> dataFiles) {
        super();
        this.fileRefenreceRequests.addAll(dataFiles);
    }

    public Set<FileStorageRequest> getFileReferenceRequests() {
        return fileRefenreceRequests;
    }

}
