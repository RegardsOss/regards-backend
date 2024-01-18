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
import fr.cnes.regards.modules.fileaccess.plugin.dto.FileDeletionRequestDto;

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
     * Notify system that the given {@link FileDeletionRequestDto} is deleted.
     *
     * @param fileDeletionRequest {@link FileDeletionRequestDto} deleted.
     */
    void deletionSucceed(FileDeletionRequestDto fileDeletionRequest);

    /**
     * Notify system that the given {@link FileDeletionRequestDto} is deleted by a pending action is remaining to fullfill
     * complete file deletion.
     *
     * @param fileDeletionRequest {@link FileDeletionRequestDto} deleted.
     */
    void deletionSucceedWithPendingAction(FileDeletionRequestDto fileDeletionRequest);

    /**
     * Notify the system that the given {@link FileReferenceDto} couldn't be deleted.
     *
     * @param fileDeletionRequest {@link FileDeletionRequestDto} not deleted.
     * @param cause               {@link String} error message.
     */
    void deletionFailed(FileDeletionRequestDto fileDeletionRequest, String cause);

}
