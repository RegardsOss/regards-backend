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
package fr.cnes.regards.modules.fileaccess.dto.output;

import fr.cnes.regards.modules.fileaccess.dto.IStoragePluginConfigurationDto;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Information for storage request that will be sent to the worker manager during storage process.
 *
 * @author Thibaud Michaudel
 **/
public class StorageWorkerRequestDto {

    private final String checksum;

    private final String algorithm;

    private final String url;

    private final Path destinationDirectory;

    private final boolean computeImageSize;

    private final IStoragePluginConfigurationDto parameters;

    public StorageWorkerRequestDto(String checksum,
                                   String algorithm,
                                   String url,
                                   Path destinationDirectory,
                                   boolean computeImageSize,
                                   IStoragePluginConfigurationDto parameters) {
        this.checksum = checksum;
        this.algorithm = algorithm;
        this.url = url;
        this.destinationDirectory = destinationDirectory;
        this.computeImageSize = computeImageSize;
        this.parameters = parameters;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getUrl() {
        return url;
    }

    public Path getDestinationDirectory() {
        return destinationDirectory;
    }

    public boolean isComputeImageSize() {
        return computeImageSize;
    }

    public IStoragePluginConfigurationDto getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageWorkerRequestDto that = (StorageWorkerRequestDto) o;
        return Objects.equals(checksum, that.checksum) && Objects.equals(algorithm, that.algorithm) && Objects.equals(
            url,
            that.url) && Objects.equals(destinationDirectory, that.destinationDirectory) && Objects.equals(parameters,
                                                                                                           that.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, algorithm, url, destinationDirectory, parameters);
    }

    @Override
    public String toString() {
        return "StorageWorkerRequestDto{"
               + "checksum='"
               + checksum
               + '\''
               + ", algorithm='"
               + algorithm
               + '\''
               + ", url='"
               + url
               + '\''
               + ", destinationDirectory="
               + destinationDirectory
               + ", parameters="
               + parameters
               + '}';
    }
}
