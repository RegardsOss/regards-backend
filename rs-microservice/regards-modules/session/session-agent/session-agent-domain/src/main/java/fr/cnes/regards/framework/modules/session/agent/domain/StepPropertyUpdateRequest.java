package fr.cnes.regards.framework.modules.session.agent.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.sessioncommons.domain.StepTypeEnum;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_step_property_update_request")
public class StepPropertyUpdateRequest {

    @Id
    @SequenceGenerator(name = "stepPropertySequence", initialValue = 1, sequenceName = "seq_step_property")
    @GeneratedValue(generator = "stepPropertySequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "step_id")
    @NotNull
    private String stepId;

    @Column(name = "source")
    @NotNull
    private String source;

    @Column(name = "session")
    @NotNull
    private String session;

    @Column(name = "date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime date;

    @Column(name = "step_type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepTypeEnum stepType;

    @Column(name = "state")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepPropertyEventStateEnum state;

    @Column(name = "property")
    @NotNull
    private String property;

    @Column(name = "value")
    @NotNull
    private String value;

    @Column(name = "type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepPropertyEventTypeEnum type;

    @Column(name = "input_related")
    @NotNull
    private boolean input_related;

    @Column(name = "output_related")
    @NotNull
    private boolean output_related;

    public StepPropertyUpdateRequest(@NotNull String stepId, @NotNull String source, @NotNull String session, OffsetDateTime date,
            @NotNull StepTypeEnum stepType, @NotNull StepPropertyEventStateEnum state, @NotNull String property,
            @NotNull String value, @NotNull StepPropertyEventTypeEnum type, @NotNull boolean input_related,
            @NotNull boolean output_related) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.date = date;
        this.stepType = stepType;
        this.state = state;
        this.property = property;
        this.value = value;
        this.type = type;
        this.input_related = input_related;
        this.output_related = output_related;
    }

    public String getStepId() {
        return stepId;
    }

    public void setStepId(String stepId) {
        this.stepId = stepId;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime date) {
        this.date = date;
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

    public StepPropertyEventTypeEnum getType() {
        return type;
    }

    public void setType(StepPropertyEventTypeEnum type) {
        this.type = type;
    }

    public boolean isInput_related() {
        return input_related;
    }

    public void setInput_related(boolean input_related) {
        this.input_related = input_related;
    }

    public boolean isOutput_related() {
        return output_related;
    }

    public void setOutput_related(boolean output_related) {
        this.output_related = output_related;
    }
}
