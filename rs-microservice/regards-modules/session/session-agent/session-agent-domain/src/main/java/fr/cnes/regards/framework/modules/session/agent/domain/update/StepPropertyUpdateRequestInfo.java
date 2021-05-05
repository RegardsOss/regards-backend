package fr.cnes.regards.framework.modules.session.agent.domain.update;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

/**
 * Store information related to {@link StepPropertyUpdateRequest}
 *
 * @author Iliana Ghazali
 **/
@Embeddable
public class StepPropertyUpdateRequestInfo {

    @Column(name = "step_type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepTypeEnum stepType;

    @Column(name = "state")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepPropertyStateEnum state;

    @Column(name = "property")
    @NotNull
    private String property;

    @Column(name = "value")
    @NotNull
    private String value;

    @Column(name = "input_related")
    @NotNull
    private boolean inputRelated;

    @Column(name = "output_related")
    @NotNull
    private boolean outputRelated;

    public StepPropertyUpdateRequestInfo(@NotNull StepTypeEnum stepType, @NotNull StepPropertyStateEnum state,
            @NotNull String property, @NotNull String value, @NotNull boolean inputRelated,
            @NotNull boolean outputRelated) {
        this.stepType = stepType;
        this.state = state;
        this.property = property;
        this.value = value;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    public StepPropertyUpdateRequestInfo() {
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
}