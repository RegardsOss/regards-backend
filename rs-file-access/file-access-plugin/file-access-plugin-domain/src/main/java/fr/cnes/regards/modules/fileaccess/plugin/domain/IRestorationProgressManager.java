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

import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileCacheRequestDto;

import jakarta.annotation.Nullable;

import java.net.URL;
import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * The ProgressManager is used by {@link IStorageLocation} plugins to notify the upper service of storage action results :
 * <ul>
 * <li>Restoration succeed in the internal cache{@link #restoreSucceededInternalCache}</li>
 * <li>Restoration succeed in the external cache{@link #restoreSucceededExternalCache}</li>
 * <li>Restoration failed {@link #restoreFailed}</li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public interface IRestorationProgressManager {

    /**
     * Notify system that the given {@link FileReferenceDto} is restored from the internal cache.
     *
     * @param fileCacheRequest file cache request restored.
     * @param restoredFilePath path of restored file
     */
    void restoreSucceededInternalCache(FileCacheRequestDto fileCacheRequest, Path restoredFilePath);

    /**
     * Notify the system that the given {FileReferenceDto} couldn't be restored.
     *
     * @param fileCacheRequest file cache request not restored.
     * @param cause            error message.
     */
    void restoreFailed(FileCacheRequestDto fileCacheRequest, String cause);

    /**
     * Notify system that the given {@link FileReferenceDto} is restored from the external cache.
     *
     * @param fileCacheRequest file cache request restored.
     * @param restoredFileUrl  url of restored file
     * @param fileSize         size of restored file
     * @param expirationDate   expiration of file in external cache
     */
    void restoreSucceededExternalCache(FileCacheRequestDto fileCacheRequest,
                                       URL restoredFileUrl,
                                       @Nullable Long fileSize,
                                       OffsetDateTime expirationDate);

}
