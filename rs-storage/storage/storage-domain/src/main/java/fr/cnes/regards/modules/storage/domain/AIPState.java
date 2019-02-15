/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain;

/**
 * Represent the state of an AIP.
 * State transition from top to bottom unless indicated otherwise.
 *
 * <pre>
 *                 o
 *                 |_______ REJECTED
 *                 |
 *               VALID <----------------  update with new files to store
 *                 |                    |
 *              PENDING                 |
 *              /     \                 |
 *             /   DATAFILES_STORED <---| update with no new files to store
 *            /         \               |
 *           /     WRITING_METADATA     |
 *          /             \             |
 * STORAGE_ERROR <-  STORING_METADATA   |
 *        |                 |           |
 *        |                 |           |
 *        |              STORED--------->  update request
 *          \             /
 *           \          /
 *            \       /
 *             DELETED
 * </pre>
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public enum AIPState implements IAipState {
    /**
     * AIP has been rejected
     */
    REJECTED,
    /**
     * AIP has been validated, network has not corrupted it
     */
    VALID,
    /**
     * Data storage has been scheduled
     */
    PENDING,
    /**
     * Data files stored. AIP is ready to store his metadata
     */
    DATAFILES_STORED,
    /**
     * Metadata has been scheduled to be written
     */
    WRITING_METADATA,
    /**
     * Metadata storage has been scheduled
     */
    STORING_METADATA,
    /**
     * Data and metadata storages have ended successfully
     */
    STORED,
    /**
     * Data or metadata storage has encountered a problem
     */
    STORAGE_ERROR,
    /**
     * AIP has been logically deleted
     */
    DELETED;

    @Override
    public String getName() {
        return this.name();
    }
}
