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
package fr.cnes.regards.modules.dam.domain.dataaccess.accessright;

/**
 * Access level on a dataset and all its data objects or one of its subset
 *
 * @author Sylvain Vissiere-Guerinet
 */
public enum AccessLevel {
    /**
     * no access at all on the dataset and so we do not have access to its data
     */
    NO_ACCESS,
    /**
     * only access to metadata of the dataset but do not have access to its data at all(metadata and data)
     */
    RESTRICTED_ACCESS,
    /**
     * Access to dataset metadata and custom access to data metadata
     */
    CUSTOM_ACCESS,
    /**
     * full access to the dataset(so the metadata of the dataset and the metadata of its data), the access to the
     * physical data of the datum is constrained by the {@link AccessRight}
     */
    FULL_ACCESS
}
