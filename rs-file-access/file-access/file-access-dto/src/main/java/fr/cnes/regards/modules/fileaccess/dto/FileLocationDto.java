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
package fr.cnes.regards.modules.fileaccess.dto;

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

    private final boolean pendingActionRemaining;

    public FileLocationDto(String storage, String url, boolean pendingActionRemaining) {
        this.storage = storage;
        this.url = url;
        this.pendingActionRemaining = pendingActionRemaining;
    }

    public FileLocationDto(String storage, String url) {
        this(storage, url, false);
    }

    public String getStorage() {
        return storage;
    }

    public String getUrl() {
        return url;
    }

    public boolean isPendingActionRemaining() {
        return pendingActionRemaining;
    }

    @Override
    public String toString() {
        return String.format("FileLocation storage=[%s] url=[%s]", this.storage, this.url);
    }

}
