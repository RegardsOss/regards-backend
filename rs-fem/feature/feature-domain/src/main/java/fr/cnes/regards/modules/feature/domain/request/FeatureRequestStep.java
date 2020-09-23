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
package fr.cnes.regards.modules.feature.domain.request;

/**
 *
 * Available steps to follow to properly handle feature request with concurrent
 * updates and remote storage.
 *
 * @author Marc SORDI
 *
 */
public enum FeatureRequestStep {

    // Not granted before db persist
    LOCAL_DENIED,

    // Request processing is delayed to handle concurrent asynchronous update
    // Manager waits for a configurable delay before scheduling feature update job.
    LOCAL_DELAYED,

    // Awaiting processing
    LOCAL_SCHEDULED,

    // ERROR
    // - update cannot be done because feature doesn't exist anymore
    LOCAL_ERROR,

    // Delete files
    REMOTE_STORAGE_DELETION_REQUESTED(true, true),
    // Store files
    REMOTE_STORAGE_REQUESTED(true, true),

    // this request handling still needs to be notified
    LOCAL_TO_BE_NOTIFIED,
    REMOTE_NOTIFICATION_REQUESTED(true, true),
    REMOTE_NOTIFICATION_SUCCESS, REMOTE_NOTIFICATION_ERROR, REMOTE_CREATION_REQUESTED, REMOTE_CREATION_ERROR;

    private boolean remote = false;

    private boolean timeout = false;

    private FeatureRequestStep() {
    }

    private FeatureRequestStep(boolean remote) {
        this.remote = remote;
    }

    private FeatureRequestStep(boolean remote, boolean timeout) {
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
