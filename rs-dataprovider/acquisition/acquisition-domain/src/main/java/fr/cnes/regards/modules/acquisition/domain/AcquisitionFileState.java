/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain;

/**
 *
 * {@link AcquisitionFile} lifecycle
 *
 * <pre>
 *             o
 *             |_______ ERROR
 *             |
 *          IN_PROGRESS
 *             |_______ INVALID
 *             |
 *           VALID
 *             |
 *         ACQUIRED
 * </pre>
 *
 * @author Christophe Mertz
 * @author Marc Sordi
 *
 */
public enum AcquisitionFileState {
    /**
     * File is detected by scanning process
     */
    IN_PROGRESS,
    /**
     * File is declared valid by validating process
     */
    VALID,
    /**
     * File is declared unvalid by validating process
     */
    INVALID,
    /**
     * File is linked to a product
     */
    ACQUIRED,
    /**
     * If error occurs during a file processing
     */
    ERROR;
}
