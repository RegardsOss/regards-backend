/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.dto;

import fr.cnes.regards.modules.storage.domain.database.FileLocation;

/**
 * DTO to represents location a file referenced in storage catalog.
 * @author Sébastien Binda
 */
public class FileLocationDTO {

    /**
     * Storage location of the file
     */
    private String storage;

    /**
     * URL of the file in the storage location
     */
    private String url;

    public static FileLocationDTO build(String storage, String url) {
        FileLocationDTO dto = new FileLocationDTO();
        dto.storage = storage;
        dto.url = url;
        return dto;
    }

    public static FileLocationDTO build(FileLocation location) {
        FileLocationDTO dto = new FileLocationDTO();
        dto.storage = location.getStorage();
        dto.url = location.getUrl();
        return dto;
    }

    public String getStorage() {
        return storage;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return String.format("FileLocation storage=[%s] url=[%s]", this.storage, this.url);
    }

}
