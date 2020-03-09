/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 *                   ACQUIRING
 *                   /      \
 *                  /        \
 *  UPDATED -- COMPLETED --> INVALID
 *     \           |
 *      \______ FINISHED
 *
 * @author Christophe Mertz
 */
public enum ProductState {

    /**
     * At least one mandatory file is missing
     */
    ACQUIRING,
    /**
     * All mandatory files are acquired
     */
    COMPLETED,
    /**
     * Mandatory and optional files are acquired
     */
    FINISHED,
    /**
     * Too many mandatory or optional have been acquired
     */
    INVALID,
    /**
     * Product was already finished or completed when we acquired a new file.
     * This state is temporary and used to avoid generating multiple version of the same SIP
     */
    UPDATED;

    @Override
    public String toString() {
        return this.name();
    }
}
