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

import java.util.List;

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
     * Request versioning mode is {@link VersioningMode#MANUAL},
     * so we need administrator decision to go further
     */
    WAITING_VERSIONING_MODE,

    /**
     * The request is waiting an event from storage to proceed
     */
    WAITING_REMOTE_STORAGE,

    /**
     * The request is waiting an event from notifier response to proceed
     */
    WAITING_NOTIFIER_RESPONSE,

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

    /**
     * List of states for IngestRequest that can potentially block another IngestRequest request.
     * IngestRequest can be processed as soon as others ones have finished to create the AIP (due to versionning and
     * replacement issues)
     * It also retrieves TO_SCHEDULE requests to ensure the current request is the oldest request Ingest received
     */
    public final static List<InternalRequestState> POTENTIALLY_BLOCKED_INGEST_REQUEST_STATES = List.of(
        InternalRequestState.TO_SCHEDULE,
        InternalRequestState.CREATED,
        InternalRequestState.RUNNING);
}
