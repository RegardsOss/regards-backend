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
package fr.cnes.regards.modules.ingest.domain.request.ingest;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Information class for ingest request in error
 *
 * @author Stephane Cortine
 **/
public class IngestRequestError {

    @NotNull(message = "Storage type is required : STORED_FILE|REFERENCED_FILE")
    private StorageType storageType;

    /**
     * Checksum of the requested file
     */
    @NotBlank(message = "Request checksum is required for IngestRequestError")
    private String requestChecksum;

    /**
     * Storage of the requested file
     */
    @NotBlank(message = "Request storage is required for IngestRequestError")
    private String requestStorage;

    public IngestRequestError(StorageType storageType, String requestChecksum, String requestStorage) {
        this.storageType = storageType;
        this.requestChecksum = requestChecksum;
        this.requestStorage = requestStorage;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public String getRequestChecksum() {
        return requestChecksum;
    }

    public String getRequestStorage() {
        return requestStorage;
    }

}
