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
import fr.cnes.regards.modules.storage.domain.database.request.FileDeletionRequest;

/**
 * The ProgressManager is used by {@link IStorageLocation} plugins to notify the upper service of storage action results :
 * <ul>
 * <li>Deletion succeed {@link #deletionSucceed}</li>
 * <li>Deletion failed {@link #deletionFailed}</li>
 * </ul>
 *
 * @author Sébastien Binda
 */
public interface IDeletionProgressManager {

    /**
     * Notify system that the given {@link FileDeletionRequest} is deleted.
     *
     * @param FileDeletionRequest {@link FileDeletionRequest} deleted.
     */
    void deletionSucceed(FileDeletionRequest fileDeletionRequest);

    /**
     * Notify system that the given {@link FileDeletionRequest} is deleted by a pending action is remaining to fullfill
     * complete file deletion.
     *
     * @param FileDeletionRequest {@link FileDeletionRequest} deleted.
     */
    void deletionSucceedWithPendingAction(FileDeletionRequest fileDeletionRequest);

    /**
     * Notify the system that the given {@link FileReference} couldn't be deleted.
     *
     * @param fileDeletionRequest {@link FileDeletionRequest} not deleted.
     * @param cause               {@link String} error message.
     */
    void deletionFailed(FileDeletionRequest fileDeletionRequest, String cause);

}
