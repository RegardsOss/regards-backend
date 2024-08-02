/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

/**
 * Enumeration of notification properties in ingest
 *
 * @author Iliana Ghazali
 **/
public enum SessionNotifierPropertyEnum {

    // Parameters to determine step properties

    TOTAL_REQUESTS("totalRequests", StepPropertyStateEnum.SUCCESS, true, false),

    REQUESTS_ERRORS("requestsErrors", StepPropertyStateEnum.ERROR, false, false),

    REQUESTS_RUNNING("requestsRunning", StepPropertyStateEnum.RUNNING, false, false),

    REFERENCED_PRODUCTS("referencedProducts", StepPropertyStateEnum.SUCCESS, false, true),

    DELETED_PRODUCTS("deletedProducts", StepPropertyStateEnum.INFO, false, false),

    NEW_PRODUCT_VERSIONS("newProductVersions", StepPropertyStateEnum.INFO, false, false),

    REPLACED_PRODUCTS("replacedProducts", StepPropertyStateEnum.INFO, false, false),

    IGNORED_PRODUCTS("ignoredProducts", StepPropertyStateEnum.INFO, false, false),

    PRODUCT_WAIT_VERSION_MODE("productWaitVersionMode", StepPropertyStateEnum.WAITING, false, false),

    POST_PROCESS_PENDING("postProcessPending", StepPropertyStateEnum.INFO, false, false),

    POST_PROCESS_SUCCESS("postProcessSuccess", StepPropertyStateEnum.INFO, false, false),

    POST_PROCESS_ERROR("postProcessErrors", StepPropertyStateEnum.INFO, false, false);

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
