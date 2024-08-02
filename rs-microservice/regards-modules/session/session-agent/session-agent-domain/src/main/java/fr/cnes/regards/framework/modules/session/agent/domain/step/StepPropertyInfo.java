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
package fr.cnes.regards.framework.modules.session.agent.domain.step;

import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;

/**
 * Store information related to {@link StepPropertyUpdateRequestEvent}
 *
 * @author Iliana Ghazali
 **/
public class StepPropertyInfo {

    /**
     * Type of the step
     */
    private StepTypeEnum stepType;

    /**
     * State of the step
     */
    private StepPropertyStateEnum state;

    /**
     * Name of the property to be modified
     */
    private String property;

    /**
     * Value of the property to modify
     */
    private String value;

    /**
     * If the step is related to an input
     */
    private boolean inputRelated;

    /**
     * If the step is related to an output
     */
    private boolean outputRelated;

    public StepPropertyInfo(StepTypeEnum stepType,
                            StepPropertyStateEnum state,
                            String property,
                            String value,
                            boolean inputRelated,
                            boolean outputRelated) {
        this.stepType = stepType;
        this.state = state;
        this.property = property;
        this.value = value;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    public StepTypeEnum getStepType() {
        return stepType;
    }

    public void setStepType(StepTypeEnum stepType) {
        this.stepType = stepType;
    }

    public StepPropertyStateEnum getState() {
        return state;
    }

    public void setState(StepPropertyStateEnum state) {
        this.state = state;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isInputRelated() {
        return inputRelated;
    }

    public void setInputRelated(boolean inputRelated) {
        this.inputRelated = inputRelated;
    }

    public boolean isOutputRelated() {
        return outputRelated;
    }

    public void setOutputRelated(boolean outputRelated) {
        this.outputRelated = outputRelated;
    }

    @Override
    public String toString() {
        return "StepPropertyInfo{"
               + "stepType="
               + stepType
               + ", state="
               + state
               + ", property='"
               + property
               + '\''
               + ", value='"
               + value
               + '\''
               + ", inputRelated="
               + inputRelated
               + ", outputRelated="
               + outputRelated
               + '}';
    }
}