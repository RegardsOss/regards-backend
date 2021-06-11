/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.feature.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

/**
 * Enumeration of all properties monitored within fem
 *
 * @author Iliana Ghazali
 **/
public enum SessionFeaturePropertyEnum {

    /**
     * Number of referencing requests
     */
    REFERENCING_REQUESTS("referencingRequests", StepPropertyStateEnum.SUCCESS, true, false),

    /**
     * Number of delete requests
     */
    DELETE_REQUESTS("deleteRequests", StepPropertyStateEnum.INFO, false, false),

    /**
     * Number of update requests
     */
    UPDATE_REQUESTS("updateRequests", StepPropertyStateEnum.INFO, false, false),

    /**
     * Number of requests notified again
     */
    NOTIFY_REQUESTS("notifyRequests", StepPropertyStateEnum.INFO, false, false),

    /**
     * Number of requests denied
     */
    REQUESTS_REFUSED("refusedRequests", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Number of requests in error
     */
    REQUESTS_ERRORS("requestsErrors", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Number of requests running
     */
    REQUESTS_RUNNING("requestsRunning", StepPropertyStateEnum.RUNNING, false, false),

    /**
     * Number of referenced products
     */
    REFERENCED_PRODUCTS("referencedProducts", StepPropertyStateEnum.SUCCESS, false, true),

    /**
     * Number of deleted products
     */
    DELETED_PRODUCTS("deletedProducts", StepPropertyStateEnum.INFO, true, false),

    /**
     * Number of products notified again
     */
    NOTIFY_PRODUCTS("notifyProducts", StepPropertyStateEnum.INFO, false, false);

    /**
     * Name of the property
     */
    private final String name;

    /**
     * Corresponding state
     */
    private final StepPropertyStateEnum state;

    /**
     * If the property is inputRelated
     */
    private final boolean inputRelated;

    /**
     * If the property is outputRelated
     */
    private final boolean outputRelated;

    SessionFeaturePropertyEnum(String name, StepPropertyStateEnum state, boolean inputRelated,
            boolean outputRelated) {
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