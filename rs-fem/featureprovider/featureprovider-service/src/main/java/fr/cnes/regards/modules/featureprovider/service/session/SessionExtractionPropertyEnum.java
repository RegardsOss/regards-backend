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
package fr.cnes.regards.modules.featureprovider.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

/**
 * Enumeration of all properties monitored within feature provider
 *
 * @author Iliana Ghazali
 **/
public enum SessionExtractionPropertyEnum {

    /**
     * Number of extraction requests received
     */
    TOTAL_REQUESTS("totalRequests", StepPropertyStateEnum.SUCCESS, true, false),

    /**
     * Number of requests currently running
     */
    REQUESTS_RUNNING("requestsRunning", StepPropertyStateEnum.RUNNING, false, false),

    /**
     * Number of generated products
     */
    GENERATED_PRODUCTS("generatedProducts", StepPropertyStateEnum.SUCCESS, false, true),

    /**
     * Number of requests refused
     */
    REQUESTS_REFUSED("requestsRefused", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Number of requests in error
     */
    REQUESTS_ERRORS("requestsErrors", StepPropertyStateEnum.ERROR, false, true);

    /**
     * Name of the property
     */
    private final String name;

    /**
     * Corresponding state
     */
    private final StepPropertyStateEnum state;

    /**
     * If the property is inputRelated, this field will be increment or decrement according to the value of the property
     */
    private final boolean inputRelated;

    /**
     * If the property is outputRelated, this field will be increment or decrement according to the value of the
     * property
     */
    private final boolean outputRelated;

    SessionExtractionPropertyEnum(String name, StepPropertyStateEnum state, boolean inputRelated,
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