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
package fr.cnes.regards.modules.filecatalog.dto;

import java.util.Objects;

/**
 * Dto of an event sent from file-catalog to file-packager in order to request the packaging of a small file
 *
 * @author Thibaud Michaudel
 **/
public class FileArchiveRequestDto {

    private final String storage;

    private final String checksum;

    private final String fileName;

    /**
     * Where the file is currently stored locally (before it is packaged and sent to the destination storage).
     */
    private final String currentFileParentPath;

    /**
     * Url where the package will be stored in the destination storage.
     */
    private final String finalArchiveParentUrl;

    private final long fileSize;

    private final long fileStorageRequestId;

    public FileArchiveRequestDto(long fileStorageRequestId,
                                 String storage,
                                 String checksum,
                                 String fileName,
                                 String currentFileParentPath,
                                 String finalArchiveParentUrl,
                                 long fileSize) {
        this.storage = storage;
        this.checksum = checksum;
        this.fileName = fileName;
        this.currentFileParentPath = currentFileParentPath;
        this.finalArchiveParentUrl = finalArchiveParentUrl;
        this.fileSize = fileSize;
        this.fileStorageRequestId = fileStorageRequestId;
    }

    public String getStorage() {
        return storage;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCurrentFileParentPath() {
        return currentFileParentPath;
    }

    public String getFinalArchiveParentUrl() {
        return finalArchiveParentUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getFileStorageRequestId() {
        return fileStorageRequestId;
    }

    @Override
    public String toString() {
        return "FileArchiveRequestDto{"
               + "storage='"
               + storage
               + '\''
               + ", checksum='"
               + checksum
               + '\''
               + ", fileName='"
               + fileName
               + '\''
               + ", storeParentPath='"
               + currentFileParentPath
               + '\''
               + ", storeParentUrl='"
               + finalArchiveParentUrl
               + '\''
               + ", fileSize="
               + fileSize
               + ", fileStorageRequestId="
               + fileStorageRequestId
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileArchiveRequestDto that = (FileArchiveRequestDto) o;
        return fileSize == that.fileSize
               && fileStorageRequestId == that.fileStorageRequestId
               && Objects.equals(storage,
                                 that.storage)
               && Objects.equals(checksum, that.checksum)
               && Objects.equals(fileName, that.fileName)
               && Objects.equals(currentFileParentPath, that.currentFileParentPath)
               && Objects.equals(finalArchiveParentUrl, that.finalArchiveParentUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage,
                            checksum,
                            fileName,
                            currentFileParentPath,
                            finalArchiveParentUrl,
                            fileSize,
                            fileStorageRequestId);
    }
}
