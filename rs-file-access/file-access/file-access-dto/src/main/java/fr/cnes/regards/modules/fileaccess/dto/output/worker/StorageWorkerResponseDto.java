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
package fr.cnes.regards.modules.fileaccess.dto.output.worker;

import fr.cnes.regards.modules.fileaccess.dto.output.worker.type.FileMetadata;
import fr.cnes.regards.modules.fileaccess.dto.output.worker.type.FileProcessingMetadata;
import org.springframework.util.Assert;

import java.beans.ConstructorProperties;
import java.util.Objects;

/**
 * Response from the worker after the processing of a storage worker request.
 *
 * @author Iliana Ghazali
 **/
public class StorageWorkerResponseDto {

    private final FileProcessingMetadata fileProcessingMetadata;

    private final FileMetadata storeFileMetadata;

    @ConstructorProperties({ "fileProcessingMetadata", "storeFileMetadata" })
    public StorageWorkerResponseDto(FileProcessingMetadata fileProcessingMetadata, FileMetadata storeFileMetadata) {
        Assert.notNull(fileProcessingMetadata, "processing metadata are mandatory!");
        Assert.notNull(storeFileMetadata, "file metadata are mandatory!");
        this.fileProcessingMetadata = fileProcessingMetadata;
        this.storeFileMetadata = storeFileMetadata;
    }

    public FileProcessingMetadata getFileProcessingMetadata() {
        return fileProcessingMetadata;
    }

    public FileMetadata getStoreFileMetadata() {
        return storeFileMetadata;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageWorkerResponseDto that = (StorageWorkerResponseDto) o;
        return Objects.equals(fileProcessingMetadata, that.fileProcessingMetadata) && Objects.equals(storeFileMetadata,
                                                                                                     that.storeFileMetadata);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileProcessingMetadata, storeFileMetadata);
    }

    @Override
    public String toString() {
        return "StorageWorkerResponseDto{"
               + "fileProcessingMetadata="
               + fileProcessingMetadata
               + ", storeFileMetadata="
               + storeFileMetadata
               + '}';
    }
}
