/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.fileaccess.plugin.dto.output;

import fr.cnes.regards.modules.filecatalog.dto.AbstractStoragePluginConfigurationDto;

import java.nio.file.Path;

/**
 * Information for storage request that will be sent to the worker manager during storage process.
 *
 * @author Thibaud Michaudel
 **/
public class StorageWorkerRequestDto {

    private final Long requestId;

    private final String storage;

    private final String checksum;

    private final String url;

    private final Path destination;

    private final AbstractStoragePluginConfigurationDto parameters;

    public StorageWorkerRequestDto(Long requestId,
                                   String storage,
                                   String checksum,
                                   String url,
                                   Path destination,
                                   AbstractStoragePluginConfigurationDto parameters) {
        this.requestId = requestId;
        this.storage = storage;
        this.checksum = checksum;
        this.url = url;
        this.destination = destination;
        this.parameters = parameters;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getStorage() {
        return storage;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getUrl() {
        return url;
    }

    public Path getDestination() {
        return destination;
    }

    public AbstractStoragePluginConfigurationDto getParameters() {
        return parameters;
    }
}
