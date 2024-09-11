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

/**
 * Status of a file being managed by the file packager.
 * Note that this enum should not be used for files not managed by the file packager (the FileArchiveStatus field should be null).
 *
 * @author Thibaud Michaudel
 **/
public enum FileArchiveStatus {
    /**
     * The file is stored in a package on the distant storage
     */
    STORED,

    /**
     * The file is stored locally and being processed by the packager
     */
    TO_STORE,

    /**
     * The file is marked to be deleted
     */
    TO_DELETE
}
