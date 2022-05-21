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
package fr.cnes.regards.modules.ingest.domain.request;

public enum InternalRequestState {

    /**
     * When the request is not scheduled yet
     */
    TO_SCHEDULE,
    /**
     * When the request is ready to be processed
     */
    CREATED,
    /**
     * Request versioning mode is {@link fr.cnes.regards.modules.ingest.domain.sip.VersioningMode#MANUAL},
     * so we need administrator decision to go further
     */
    WAITING_VERSIONING_MODE,
    /**
     * When the request cannot be processed for now
     */
    BLOCKED,
    /**
     * When the request is running
     */
    RUNNING,
    /**
     * When the request stopped with an error
     */
    ERROR, ABORTED, IGNORED;
}
