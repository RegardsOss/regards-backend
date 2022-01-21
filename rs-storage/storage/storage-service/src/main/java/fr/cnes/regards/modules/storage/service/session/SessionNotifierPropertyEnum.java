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
package fr.cnes.regards.modules.storage.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

/**
 * Enumeration for all properties monitored within storage
 *
 * @author Iliana Ghazali
 **/
public enum SessionNotifierPropertyEnum {

    /**
     * Number of reference requests
     */
    REFERENCE_REQUESTS("referenceRequests", StepPropertyStateEnum.SUCCESS, true, false),

    /**
     * Number of storage requests
     */
    STORE_REQUESTS("storeRequests", StepPropertyStateEnum.SUCCESS, true, false),

    /**
     * Number of copy requests
     */
    COPY_REQUESTS("copyRequests", StepPropertyStateEnum.SUCCESS, false, false),

    /**
     * Number of delete requests
     */
    DELETE_REQUESTS("deleteRequests", StepPropertyStateEnum.SUCCESS, false, false),

    /**
     * Number of requests refused
     */
    REQUESTS_RUNNING("requestsRunning", StepPropertyStateEnum.RUNNING, false, false),

    /**
     * Number of requests refused
     */
    REQUESTS_REFUSED("requestsRefused", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Number of requests in error
     */
    REQUESTS_ERRORS("requestsErrors", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Number of stored files
     */
    STORED_FILES("storedFiles", StepPropertyStateEnum.SUCCESS, false, true),

    /**
     * Number of referenced files following a referencing request
     */
    REFERENCED_FILES("referencedFiles", StepPropertyStateEnum.SUCCESS, false, true),

    /**
     * Number of deleted files
     */
    DELETED_FILES("deletedFiles", StepPropertyStateEnum.INFO, false, false);

    private final String name;

    private final StepPropertyStateEnum state;

    private final boolean inputRelated;

    private final boolean outputRelated;

    SessionNotifierPropertyEnum(String name, StepPropertyStateEnum state, boolean inputRelated, boolean outputRelated) {
        this.name = name;
        this.state = state;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    public String getName() {
        return name;
    }

    public StepPropertyStateEnum getState() {
        return state;
    }

    public boolean isInputRelated() {
        return inputRelated;
    }

    public boolean isOutputRelated() {
        return outputRelated;
    }
}
