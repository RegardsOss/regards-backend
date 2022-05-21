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
package fr.cnes.regards.modules.feature.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

public enum FeatureSessionProperty {

    REFERENCING_REQUESTS("referencingRequests", StepPropertyStateEnum.SUCCESS, true, false), DELETE_REQUESTS(
        "deleteRequests",
        StepPropertyStateEnum.INFO), UPDATE_REQUESTS("updateRequests", StepPropertyStateEnum.INFO), NOTIFY_REQUESTS(
        "notifyRequests",
        StepPropertyStateEnum.INFO),

    REFERENCED_PRODUCTS("referencedProducts", StepPropertyStateEnum.SUCCESS, false, true), DELETED_PRODUCTS(
        "deletedProducts",
        StepPropertyStateEnum.INFO), UPDATED_PRODUCTS("updatedProducts", StepPropertyStateEnum.INFO), NOTIFY_PRODUCTS(
        "notifyProducts",
        StepPropertyStateEnum.INFO),

    RUNNING_REFERENCING_REQUESTS("runningReferencingRequests", StepPropertyStateEnum.RUNNING), RUNNING_DELETE_REQUESTS(
        "runningDeleteRequests",
        StepPropertyStateEnum.RUNNING), RUNNING_UPDATE_REQUESTS("runningUpdateRequests",
                                                                StepPropertyStateEnum.RUNNING), RUNNING_NOTIFY_REQUESTS(
        "runningNotifyRequests",
        StepPropertyStateEnum.RUNNING),

    DENIED_REFERENCING_REQUESTS("deniedReferencingRequests", StepPropertyStateEnum.ERROR), DENIED_DELETE_REQUESTS(
        "deniedDeleteRequests",
        StepPropertyStateEnum.ERROR), DENIED_UPDATE_REQUESTS("deniedUpdateRequests",
                                                             StepPropertyStateEnum.ERROR), DENIED_NOTIFY_REQUESTS(
        "deniedNotifyRequests",
        StepPropertyStateEnum.ERROR),

    IN_ERROR_REFERENCING_REQUESTS("inErrorReferencingRequests", StepPropertyStateEnum.ERROR), IN_ERROR_DELETE_REQUESTS(
        "inErrorDeleteRequests",
        StepPropertyStateEnum.ERROR), IN_ERROR_UPDATE_REQUESTS("inErrorUpdateRequests",
                                                               StepPropertyStateEnum.ERROR), IN_ERROR_NOTIFY_REQUESTS(
        "inErrorNotifyRequests",
        StepPropertyStateEnum.ERROR),

    RUNNING_DISSEMINATION_PRODUCTS("%s.pending", StepPropertyStateEnum.RUNNING, true, false), DISSEMINATED_PRODUCTS(
        "%s.done",
        StepPropertyStateEnum.INFO,
        false,
        true);

    private final String name;

    private final StepPropertyStateEnum state;

    private boolean inputRelated;

    private boolean outputRelated;

    FeatureSessionProperty(String name, StepPropertyStateEnum state, boolean inputRelated, boolean outputRelated) {
        this.name = name;
        this.state = state;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    FeatureSessionProperty(String name, StepPropertyStateEnum state) {
        this.name = name;
        this.state = state;
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
