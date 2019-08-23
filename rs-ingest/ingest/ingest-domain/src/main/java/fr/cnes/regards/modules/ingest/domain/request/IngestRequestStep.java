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
package fr.cnes.regards.modules.ingest.domain.request;

/**
 * Available steps to follow to properly handle SIP ingestion
 * with local AIP generation and remote storage.
 *
 * @author Marc SORDI
 */
public enum IngestRequestStep {

    // Not granted before db persist
    LOCAL_DENIED(false),

    // Awaiting processing
    LOCAL_SCHEDULED(false),

    // Synchronous ingest processing job steps
    LOCAL_INIT(false),
    LOCAL_PRE_PROCESSING(false),
    LOCAL_VALIDATION(false),
    LOCAL_GENERATION(false),
    LOCAL_TAGGING(false),
    LOCAL_POST_PROCESSING(false),
    LOCAL_FINAL(false),

    // Remote and asynchronous storage steps

    // For AIP files
    REMOTE_STORAGE_REQUESTED(true),
    REMOTE_STORAGE_GRANTED(true),
    REMOTE_STORAGE_DENIED(true),
    REMOTE_STORAGE_ERROR(true),
    REMOTE_STORAGE_SUCCESS(true),

    // For AIP itself
    REMOTE_AIP_STORAGE_REQUESTED(true),
    REMOTE_AIP_STORAGE_GRANTED(true),
    REMOTE_AIP_STORAGE_DENIED(true),
    REMOTE_AIP_STORAGE_ERROR(true),
    REMOTE_AIP_STORAGE_SUCCESS(true);

    private boolean remote = false;

    private IngestRequestStep(boolean remote) {
        this.remote = remote;
    }

    /**
     * Identify a remote step
     */
    public boolean isRemote() {
        return remote;
    }
}
