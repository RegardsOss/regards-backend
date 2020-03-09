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
 * but WITHOUT ANY WARRANTY), without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.service.session;

import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationState;

/**
 * Enumeration for all product status in sessions.
 *
 * @author Sébastien Binda
 *
 */
public enum SessionProductPropertyEnum {

    /**
     * Name of the property that collects number of products generated
     */
    PROPERTY_GENERATED("generated", SessionNotificationState.OK),

    /**
     * Name of the property that collects number of products incomplete
     */
    PROPERTY_INCOMPLETE("incomplete", SessionNotificationState.OK),

    /**
     * Name of the property that collects number of products invalid (too many files attached to a single product)
     */
    PROPERTY_INVALID("invalid", SessionNotificationState.ERROR),

    /**
     * Name of the property that collects number of products generated
     */
    PROPERTY_GENERATION_ERROR("generation_error", SessionNotificationState.ERROR),

    PROPERTY_INGESTION_FAILED("ingestion_failed", SessionNotificationState.OK),

    PROPERTY_INGESTED("ingested", SessionNotificationState.OK),

    PROPERTY_FILES_ACQUIRED("files_acquired", SessionNotificationState.OK),

    PROPERTY_COMPLETED("complete", SessionNotificationState.OK);

    private String value = null;

    private SessionNotificationState state = SessionNotificationState.OK;

    SessionProductPropertyEnum(String value, SessionNotificationState state) {
        this.value = value;
        this.state = state;
    }

    public String getValue() {
        return value;
    }

    public SessionNotificationState getState() {
        return state;
    }

}
