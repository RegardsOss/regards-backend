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
package fr.cnes.regards.modules.fileaccess.dto;

import java.util.Objects;

/**
 * Dto to represents location a file referenced in storage catalog.
 *
 * @author Sébastien Binda
 */
public class FileLocationDto {

    /**
     * Storage location of the file
     */
    private final String storage;

    /**
     * URL of the file in the storage location
     */
    private final String url;

    /**
     * Status of the file inside its small file archive, null if the file is not a small file in a tier 3 storage.
     */
    private final FileArchiveStatus fileArchiveStatus;

    public FileLocationDto(String storage, String url, FileArchiveStatus fileArchiveStatus) {
        this.storage = storage;
        this.url = url;
        this.fileArchiveStatus = fileArchiveStatus;
    }

    public FileLocationDto(String storage, String url) {
        this(storage, url, null);
    }

    public String getStorage() {
        return storage;
    }

    public String getUrl() {
        return url;
    }

    public FileArchiveStatus getFileArchiveStatus() {
        return fileArchiveStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileLocationDto that = (FileLocationDto) o;
        return Objects.equals(storage, that.storage)
               && Objects.equals(url, that.url)
               && fileArchiveStatus == that.fileArchiveStatus;
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage, url, fileArchiveStatus);
    }

    @Override
    public String toString() {
        return "FileLocationDto{"
               + "storage='"
               + storage
               + '\''
               + ", url='"
               + url
               + '\''
               + ", fileArchiveStatus="
               + fileArchiveStatus
               + '}';
    }
}
