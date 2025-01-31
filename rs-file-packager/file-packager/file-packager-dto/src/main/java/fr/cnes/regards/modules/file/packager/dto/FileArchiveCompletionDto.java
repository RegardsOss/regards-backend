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
package fr.cnes.regards.modules.file.packager.dto;

/**
 * Information about a file whose archive has been fully stored
 *
 * @author Thibaud Michaudel
 */
public class FileArchiveCompletionDto {

    private String storage;

    private String checksum;

    public FileArchiveCompletionDto(String storage, String checksum) {
        this.storage = storage;
        this.checksum = checksum;
    }

    public String getStorage() {
        return storage;
    }

    public String getChecksum() {
        return checksum;
    }
}
