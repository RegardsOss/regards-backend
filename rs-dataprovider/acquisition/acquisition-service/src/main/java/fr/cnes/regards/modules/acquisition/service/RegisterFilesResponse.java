/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import java.time.OffsetDateTime;

/**
 * Object containing acquisition files registration informations
 *
 * @author Sébastien Binda
 *
 */
public class RegisterFilesResponse {

    /**
     * Number of files registered by the registration process
     */
    private long numberOfRegisteredFiles = 0L;

    /**
     * Last update date of the newest registered file
     */
    private OffsetDateTime lastUpdateDate = null;

    private boolean hasNext = false;

    private RegisterFilesResponse() {
    }

    /**
     *
     * @param numberOfRegisteredFiles Number of files registered by the registration process
     * @param lastUpdateDate Last update date of the newest registered file
     * @return {@link RegisterFilesResponse}
     */
    public static RegisterFilesResponse build(long numberOfRegisteredFiles, OffsetDateTime lastUpdateDate,
            boolean hasNext) {
        RegisterFilesResponse response = new RegisterFilesResponse();
        response.numberOfRegisteredFiles = numberOfRegisteredFiles;
        response.lastUpdateDate = lastUpdateDate;
        response.hasNext = hasNext;
        return response;
    }

    public long getNumberOfRegisteredFiles() {
        return numberOfRegisteredFiles;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public boolean hasNext() {
        return hasNext;
    }

}
