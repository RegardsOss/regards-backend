/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.modules.ingest.domain.entity;

/**
 *
 * AIP lifecycle
 *
 * <pre>
 *             o
 *             |
 *          CREATED
 *             |
 *    STORAGE_REQUEST_DONE
 *             |____________STORAGE_REQUEST_DENIED
 *             |
 *   STORAGE_REQUEST_GRANTED
 *             |____________STORAGE_ERROR
 *             |
 *   MTD_STORAGE_REQUEST_DONE
 *             |____________MTD_STORAGE_REQUEST_DENIED
 *             |
 *   MTD_STORAGE_REQUEST_GRANTED
 *             |____________MTD_STORAGE_ERROR
 *             |
 *           STORED
 *             |
 *       TO_BE_DELETED
 *             |
 *    DELETION_REQUEST_DONE
 *             |____________DELETION_REQUEST_DENIED
 *             |
 *   DELETION_REQUEST_GRANTED
 *             |
 *          DELETED
 * </pre>
 *
 * @author Marc Sordi
 *
 */
public enum AIPState {
    /**
     * AIP has been created by ingest processing chain and is ready to be stored
     */
    CREATED,
    /**
     * Storage request for files workflow
     */
    STORAGE_REQUEST_DONE,
    STORAGE_REQUEST_DENIED,
    STORAGE_REQUEST_GRANTED,
    /**
     * Error in files storage
     */
    STORAGE_ERROR,
    /**
     * Storage request for AIP file workflow
     */
    MTD_STORAGE_REQUEST_DONE,
    MTD_STORAGE_REQUEST_DENIED,
    MTD_STORAGE_REQUEST_GRANTED,
    /**
     * Error in metadata storage
     */
    MTD_STORAGE_ERROR,
    STORED,
    /**
     * AIP is marked for deletion
     */
    TO_BE_DELETED,
    /**
     * Deletion request for all AIP files workflow
     */
    DELETION_REQUEST_DONE,
    DELETION_REQUEST_DENIED,
    DELETION_REQUEST_GRANTED,
    /**
     * AIP files are deleted
     */
    DELETED;
}
