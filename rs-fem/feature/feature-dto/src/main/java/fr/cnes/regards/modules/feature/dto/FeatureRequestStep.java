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
package fr.cnes.regards.modules.feature.dto;

/**
 * Available steps to follow to properly handle feature request with concurrent
 * updates and remote storage.
 *
 * @author Marc SORDI
 */
public enum FeatureRequestStep {

    // Not granted before db persist
    LOCAL_DENIED(false, false),

    // Request processing is delayed to handle concurrent asynchronous update
    // Manager waits for a configurable delay before scheduling feature update job.
    LOCAL_DELAYED(false, false),

    // Awaiting processing
    LOCAL_SCHEDULED(true, false),

    // ERROR
    // - update cannot be done because feature doesn't exist anymore
    LOCAL_ERROR(false, true),

    LOCAL_NOTIFICATION_ERROR(false, true),

    // Delete files
    REMOTE_STORAGE_DELETION_REQUESTED(true, true, true, false), // Store files
    REMOTE_STORAGE_REQUESTED(true, true, true, false),

    // this request handling still needs to be notified
    LOCAL_TO_BE_NOTIFIED(false, false),

    REMOTE_NOTIFICATION_REQUESTED(true, true, true, false),

    REMOTE_NOTIFICATION_ERROR(false, true),

    REMOTE_STORAGE_ERROR(false, true),

    WAITING_BLOCKING_DISSEMINATION(true, false);

    private boolean remote = false;

    private final boolean processing;

    private boolean timeout = false;

    private final boolean retryableErrorStep;

    FeatureRequestStep(boolean processing, boolean retryableErrorStep) {
        this.processing = processing;
        this.retryableErrorStep = retryableErrorStep;
    }

    FeatureRequestStep(boolean processing, boolean remote, boolean timeout, boolean retryableErrorStep) {
        this.remote = remote;
        this.timeout = timeout;
        this.processing = processing;
        this.retryableErrorStep = retryableErrorStep;
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

    public boolean isProcessing() {
        return processing;
    }

    public boolean isRetryableErrorStep() {
        return retryableErrorStep;
    }

}
