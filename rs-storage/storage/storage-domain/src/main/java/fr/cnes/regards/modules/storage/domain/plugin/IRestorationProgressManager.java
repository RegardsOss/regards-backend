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

import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.request.FileCacheRequest;

import java.nio.file.Path;

/**
 * The ProgressManager is used by {@link IStorageLocation} plugins to notidy the upper service of storage action results :
 * <ul>
 * <li>Restoration succeed {@link #restoreSucceed}</li>
 * <li>Restoration failed {@link #restoreFailed}</li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public interface IRestorationProgressManager {

    /**
     * Notify system that the given {@link FileReference} is restored.
     *
     * @param FileCacheRequest {@link FileCacheRequest} restored.
     */
    public void restoreSucceed(FileCacheRequest fileRequest, Path restoredFilePath);

    /**
     * Notify the system that the given {@link FileReference} couldn't be restored.
     *
     * @param FileCacheRequest {@link FileCacheRequest} not restored.
     * @param cause            {@link String} error message.
     */
    public void restoreFailed(FileCacheRequest fileRequest, String cause);

}
