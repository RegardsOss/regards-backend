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
package fr.cnes.regards.modules.ingest.domain.request.ingest;

/**
 * Available steps to follow to properly handle SIP ingestion
 * with local AIP generation and remote storage.
 *
 * @author Marc SORDI
 */
public enum IngestRequestStep {

    // Not granted before db persist
    LOCAL_DENIED,

    // Awaiting processing
    LOCAL_SCHEDULED,

    /**
     * Synchronous ingest processing job steps
     * <p>
     * Request created with {@link #LOCAL_SCHEDULED} step
     * |
     * {@link #LOCAL_INIT}
     * |
     * {@link #LOCAL_PRE_PROCESSING}
     * |
     * {@link #LOCAL_VALIDATION}
     * |
     * {@link #LOCAL_GENERATION}
     * |
     * {@link #LOCAL_AIP_STORAGE_METADATA_UPDATE}
     * |
     * {@link #LOCAL_TAGGING}
     * |
     * {@link #LOCAL_POST_PROCESSING}
     * |
     * {@link #LOCAL_FINAL}
     */
    LOCAL_INIT,
    LOCAL_PRE_PROCESSING,
    LOCAL_VALIDATION,
    LOCAL_GENERATION,
    LOCAL_AIP_STORAGE_METADATA_UPDATE,
    LOCAL_TAGGING,
    LOCAL_POST_PROCESSING,
    LOCAL_FINAL,

    /**
     * Notification with NOTIFIER service
     */
    LOCAL_TO_BE_NOTIFIED,
    REMOTE_NOTIFICATION_ERROR,

    /**
     * Remote and asynchronous storage steps with STORAGE service
     * <p>
     * |
     * {@link #REMOTE_STORAGE_REQUESTED}
     * |_ {@link #REMOTE_STORAGE_DENIED}
     * |_ {@link #REMOTE_STORAGE_ERROR}
     * |
     * Request deleted
     */

    // For AIP files
    REMOTE_STORAGE_REQUESTED(true, true),
    REMOTE_STORAGE_DENIED(true),
    REMOTE_STORAGE_ERROR(true);

    private boolean remote = false;

    private boolean timeout = false;

    private IngestRequestStep() {
    }

    private IngestRequestStep(boolean remote) {
        this.remote = remote;
    }

    private IngestRequestStep(boolean remote, boolean timeout) {
        this.remote = remote;
        this.timeout = timeout;
    }

    /**
     * Identify a remote step
     */
    public boolean isRemote() {
        return remote;
    }

    /**
     * Identify a remote step with request timeout
     */
    public boolean withTimeout() {
        return timeout;
    }
}
