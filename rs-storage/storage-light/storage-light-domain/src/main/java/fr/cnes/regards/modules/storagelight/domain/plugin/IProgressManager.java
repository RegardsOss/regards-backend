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

import java.nio.file.Path;

import fr.cnes.regards.modules.storagelight.domain.database.FileReference;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceRequest;

/**
 * The ProgressManager is used by {@link IDataStorage} plugins to notidy the upper service of storage action results :
 * <ul>
 * <li>Storage succeed {@link #storageSucceed}</li>
 * <li>Storage failed {@link #storageFailed}</li>
 * <li>Restoration succeed {@link #restoreSucceed}</li>
 * <li>Restoration failed {@link #restoreFailed}</li>
 * <li>Deletion succeed {@link #deletionSucceed}</li>
 * <li>Deletion failed {@link #deletionFailed}</li>
 * </ul>
 * @author Sébastien Binda
 */
public interface IProgressManager {

    /**
     * Notify system that the given {@link FileReferenceRequest} is stored.
     * @param fileReferenceRequest {@link FileReferenceRequest} stored.
     */
    public void storageSucceed(FileReferenceRequest fileReferenceRequest, Long fileSize);

    /**
     * Notify the system that the given {@link FileReferenceRequest} couldn't be stored.
     * @param fileReferenceRequest {@link FileReferenceRequest} not stored.
     * @param cause {@link String} error message.
     */
    public void storageFailed(FileReferenceRequest fileReferenceRequest, String cause);

    /**
     * Notify system that the given {@link FileReference} is deleted.
     * @param fileReference {@link FileReference} deleted.
     */
    public void deletionSucceed(FileReference fileReference);

    /**
     * Notify the system that the given {@link FileReference} couldn't be deleted.
     * @param fileReference {@link FileReference} not deleted.
     * @param cause {@link String} error message.
     */
    public void deletionFailed(FileReference fileReference, String cause);

    /**
     * Notify system that the given {@link FileReference} is restored.
     * @param fileReference {@link FileReference} restored.
     * @param restoredFilePath {@link Path} of the restored file.
     */
    public void restoreSucceed(FileReference fileReference, Path restoredFilePath);

    /**
     * Notify the system that the given {@link FileReference} couldn't be restored.
     * @param fileReference {@link FileReference} not restored.
     * @param cause {@link String} error message.
     */
    public void restoreFailed(FileReference fileReference, String cause);

}
