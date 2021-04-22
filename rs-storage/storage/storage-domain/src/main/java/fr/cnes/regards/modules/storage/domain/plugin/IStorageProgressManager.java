/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.net.URL;

import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequest;

/**
 * The ProgressManager is used by {@link IStorageLocation} plugins to notidy the upper service of storage action results :
 * <ul>
 * <li>Storage succeed {@link #storageSucceed}</li>
 * <li>Storage failed {@link #storageFailed}</li>
 * </ul>
 * @author Sébastien Binda
 */
public interface IStorageProgressManager {

    /**
     * Notify system that the given {@link FileStorageRequest} is stored.
     * @param fileReferenceRequest {@link FileStorageRequest} stored.
     */
    public void storageSucceed(FileStorageRequest fileReferenceRequest, URL storedUrl, Long fileSize);

    /**
     * Notify the system that the given {@link FileStorageRequest} couldn't be stored.
     * @param fileReferenceRequest {@link FileStorageRequest} not stored.
     * @param cause {@link String} error message.
     */
    public void storageFailed(FileStorageRequest fileReferenceRequest, String cause);

}
