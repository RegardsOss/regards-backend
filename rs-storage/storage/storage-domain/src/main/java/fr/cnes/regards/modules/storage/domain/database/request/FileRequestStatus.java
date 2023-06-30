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
package fr.cnes.regards.modules.storage.domain.database.request;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

import java.util.Set;

/**
 * Enumeration for possible status of a {@link FileReference}
 *
 * @author Sébastien Binda
 */
public enum FileRequestStatus {

    /**
     * Request can be handled.
     */
    TO_DO,

    /**
     * Request has been handled.
     */
    PENDING,

    /**
     * Request is delayed, waiting to be reactivated.
     */
    DELAYED,

    /**
     * Request is finished in error.
     */
    ERROR;

    public final static Set<FileRequestStatus> RUNNING_STATUS = Sets.newHashSet(FileRequestStatus.TO_DO,
            FileRequestStatus.PENDING);

    public final static Set<FileRequestStatus> RUNNING_AND_DELAYED_STATUS = Sets.newHashSet(FileRequestStatus.TO_DO,
            FileRequestStatus.PENDING, FileRequestStatus.DELAYED);

}
