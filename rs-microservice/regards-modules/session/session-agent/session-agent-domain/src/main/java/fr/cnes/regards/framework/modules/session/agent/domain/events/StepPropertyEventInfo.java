package fr.cnes.regards.framework.modules.session.agent.domain.events;

import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;

/**
 * Store information related to {@link StepPropertyUpdateRequestEvent}
 *
 * @author Iliana Ghazali
 **/
public class StepPropertyEventInfo {

    /**
     * Type of the step
     */
    private StepTypeEnum stepType;

    /**
     * State of the step
     */
    private StepPropertyEventStateEnum state;

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

    public StepPropertyEventInfo(StepTypeEnum stepType, StepPropertyEventStateEnum state, String property, String value,
            boolean inputRelated, boolean outputRelated) {
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

    public StepPropertyEventStateEnum getState() {
        return state;
    }

    public void setState(StepPropertyEventStateEnum state) {
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
}
