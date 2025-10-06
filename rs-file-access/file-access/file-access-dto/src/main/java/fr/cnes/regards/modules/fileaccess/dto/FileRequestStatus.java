/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.dto;

import java.util.Set;

/**
 * Enumeration of possible status for entity requests or dto requests like file reference, file storage, file deletion,
 * file copy.
 * <br />
 * For a {@link fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation} the status follow the sequence:
 * <ul>
 *     <li>TO_DO -> DELAYED -> TO_DO -> PENDING -> ERROR or request immediately deleted on success</li>
 *     <li>TO_DO -> PENDING -> ERROR or request deleted</li>
 * </ul>
 * <br />
 * For a {@link fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation} the status follow the sequence:
 * <ul>
 *      <li>TO_DO -> DELAYED -> TO_DO -> ERROR or SUCCESS</li>
 *      <li>TO_DO -> ERROR or SUCCESS</li>
 * </ul>
 * <br />
 * requests FileStorageRequestAggregation and FileReferenceRequestAggregation in SUCCESS or ERROR status are deleted
 * later by the group scheduler CheckRequestDoneGroupsScheduler.checkRequestsDoneGroups
 *
 * @author Sébastien Binda
 */
public enum FileRequestStatus {

    /**
     * Request can be handled.
     */
    TO_DO,

    /**
     * Request has been handled but not completed yet.<br />
     * Status not used by
     * {@link fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation}
     * but by {@link fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation}
     */
    PENDING,

    /**
     * Request is delayed, waiting to be reactivated.
     */
    DELAYED,

    /**
     * Request is finished in error.
     */
    ERROR,

    /**
     * Request is finished in success.<br />
     * Status currently used only by {@link fr.cnes.regards.modules.storage.domain.database.request.FileReferenceRequestAggregation}
     */
    SUCCESS;

    public static final Set<FileRequestStatus> RUNNING_STATUS = Set.of(FileRequestStatus.TO_DO,
                                                                       FileRequestStatus.PENDING);

    public static final Set<FileRequestStatus> RUNNING_AND_DELAYED_STATUS = Set.of(FileRequestStatus.TO_DO,
                                                                                   FileRequestStatus.PENDING,
                                                                                   FileRequestStatus.DELAYED);

    public static boolean isRunning(FileRequestStatus status) {
        return RUNNING_STATUS.contains(status);
    }

    public static boolean isFinished(FileRequestStatus status) {
        return status == ERROR || status == SUCCESS;
    }
}
