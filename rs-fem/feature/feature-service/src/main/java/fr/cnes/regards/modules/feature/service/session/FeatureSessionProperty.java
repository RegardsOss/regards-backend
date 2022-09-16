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

    /**
     * Property to count number of reference request received
     * EventType : SUCCESS
     * InputRelated  : True
     * OutputRelated : False
     */
    REFERENCING_REQUESTS("referencingRequests", StepPropertyStateEnum.SUCCESS, true, false),

    /**
     * Property to count number of delete request received
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    DELETE_REQUESTS("deleteRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number of update request received
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    UPDATE_REQUESTS("updateRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number of notification request received
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    NOTIFY_REQUESTS("notifyRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number of referenced products
     * EventType : SUCCESS
     * InputRelated  : False
     * OutputRelated : True
     */
    REFERENCED_PRODUCTS("referencedProducts", StepPropertyStateEnum.SUCCESS, false, true),

    /**
     * Property to count number of deleted products
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    DELETED_PRODUCTS("deletedProducts", StepPropertyStateEnum.INFO),

    /**
     * Property to count number of updated products
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    UPDATED_PRODUCTS("updatedProducts", StepPropertyStateEnum.INFO),

    /**
     * Property to count number of notified products
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    NOTIFY_PRODUCTS("notifyProducts", StepPropertyStateEnum.INFO),

    /**
     * Property to count number reference request running
     * EventType : RUNNING
     * InputRelated  : False
     * OutputRelated : False
     */
    RUNNING_REFERENCING_REQUESTS("runningReferencingRequests", StepPropertyStateEnum.RUNNING),

    /**
     * Property to count number deletion request running
     * EventType : RUNNING
     * InputRelated  : False
     * OutputRelated : False
     */
    RUNNING_DELETE_REQUESTS("runningDeleteRequests", StepPropertyStateEnum.RUNNING),

    /**
     * Property to count number update request running
     * EventType : RUNNING
     * InputRelated  : False
     * OutputRelated : False
     */
    RUNNING_UPDATE_REQUESTS("runningUpdateRequests", StepPropertyStateEnum.RUNNING),

    /**
     * Property to count number notification request running
     * EventType : RUNNING
     * InputRelated  : False
     * OutputRelated : False
     */
    RUNNING_NOTIFY_REQUESTS("runningNotifyRequests", StepPropertyStateEnum.RUNNING),

    /**
     * Property to count number reference request denied
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    DENIED_REFERENCING_REQUESTS("deniedReferencingRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number delete request denied
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    DENIED_DELETE_REQUESTS("deniedDeleteRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number update request denied
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    DENIED_UPDATE_REQUESTS("deniedUpdateRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number notification request denied
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : False
     */
    DENIED_NOTIFY_REQUESTS("deniedNotifyRequests", StepPropertyStateEnum.INFO),

    /**
     * Property to count number reference request finished in error status
     * EventType : ERROR
     * InputRelated  : False
     * OutputRelated : False
     */
    IN_ERROR_REFERENCING_REQUESTS("inErrorReferencingRequests", StepPropertyStateEnum.ERROR),

    /**
     * Property to count number of delete request finished in error status
     * EventType : ERROR
     * InputRelated  : False
     * OutputRelated : False
     */
    IN_ERROR_DELETE_REQUESTS("inErrorDeleteRequests", StepPropertyStateEnum.ERROR),

    /**
     * Property to count number of update request finished in error status
     * EventType : ERROR
     * InputRelated  : False
     * OutputRelated : False
     */
    IN_ERROR_UPDATE_REQUESTS("inErrorUpdateRequests", StepPropertyStateEnum.ERROR),

    /**
     * Property to count number of notification request finished in error status
     * EventType : ERROR
     * InputRelated  : False
     * OutputRelated : False
     */
    IN_ERROR_NOTIFY_REQUESTS("inErrorNotifyRequests", StepPropertyStateEnum.ERROR),

    /**
     * Property to count number of products waiting for dissemination
     * EventType : RUNNING
     * InputRelated  : True
     * OutputRelated : False
     */
    RUNNING_DISSEMINATION_PRODUCTS("%s.pending", StepPropertyStateEnum.RUNNING, true, false),

    /**
     * Property to count number of products dissemination in success
     * EventType : INFO
     * InputRelated  : False
     * OutputRelated : True
     */
    DISSEMINATED_PRODUCTS("%s.done", StepPropertyStateEnum.INFO, false, true);

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
