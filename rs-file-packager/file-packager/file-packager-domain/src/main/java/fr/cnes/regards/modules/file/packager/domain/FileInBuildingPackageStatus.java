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
package fr.cnes.regards.modules.file.packager.domain;

/**
 * Status of a {@link FileInBuildingPackage}
 *
 * @author Thibaud Michaudel
 **/
public enum FileInBuildingPackageStatus {
    /**
     * The file is waiting to be associated with a package
     */
    WAITING_PACKAGE,

    /**
     * The file has been associated to a package that is still in construction
     */
    BUILDING,

    /**
     * The package is stored and the local file can now be deleted
     */
    TO_LOCAL_DELETE,

    /**
     * The file is being deleted
     */
    DELETING,

    /**
     * There was an error during the file deletion
     */
    DELETION_ERROR
}
