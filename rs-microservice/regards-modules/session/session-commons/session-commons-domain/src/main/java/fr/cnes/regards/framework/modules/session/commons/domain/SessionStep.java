package fr.cnes.regards.framework.modules.session.commons.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import org.checkerframework.common.aliasing.qual.Unique;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * A {@link SessionStep} represents a step in which data are added or processed. Currently 4 steps mainly exist to
 * process data during its life cycle (acquisition/referencing/storage/dissemination). They are created or updated
 * through step property events from the session agent.
 *
 * @author Iliana Ghazali
 **/
@Entity
@TypeDefs({ @TypeDef(name = "jsonb", typeClass = JsonBinaryType.class) })
@Table(name = "t_session_step")
public class SessionStep {

    /**
     * Id of the SessionStep
     */
    @Id
    @SequenceGenerator(name = "sessionStepSequence", initialValue = 1, sequenceName = "seq_session_step")
    @GeneratedValue(generator = "sessionStepSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Unique step identifier
     */
    @Column(name = "step_id")
    @Unique
    @NotNull
    private String stepId;

    /**
     * Name of the source
     */
    @Column(name = "source")
    @NotNull
    private String source;

    /**
     * Name of the session
     */
    @Column(name = "session")
    @NotNull
    private String session;

    /**
     * Type of the step. It depends on which microservice has initiated the step.
     */
    @Column(name = "type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepTypeEnum type;

    /**
     * Sum of step inputs. Accumulation of inputs from StepPropertyUpdateEventRequest
     */
    @Column(name = "input_related")
    @NotNull
    private long inputRelated = 0L;

    /**
     * Sum of step outputs. Accumulation of outputs from StepPropertyUpdateEventRequest
     */
    @Column(name = "output_related")
    @NotNull
    private long outputRelated = 0L;

    /**
     * Current state of the SessionStep. Number steps waiting or in error state and running state.
     */
    @Embedded
    @NotNull
    private StepState state;

    /**
     * Set of property/value retrieved from StepPropertyUpdateEventRequests
     */
    @Column(name = "properties", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private SessionStepProperties properties;

    /**
     * Most recent StepPropertyUpdateEventRequest
     */
    @Column(name = "last_update")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdate;

    public SessionStep(@Unique String stepId, @NotNull String source, @NotNull String session,
            @NotNull StepTypeEnum type, @NotNull StepState state, SessionStepProperties properties) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.type = type;
        this.state = state;
        this.properties = properties;
    }

    public SessionStep() {
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

    public StepTypeEnum getType() {
        return type;
    }

    public void setType(StepTypeEnum type) {
        this.type = type;
    }

    public long getInputRelated() {
        return inputRelated;
    }

    public void setInputRelated(long inputRelated) {
        this.inputRelated = inputRelated;
    }

    public long getOutputRelated() {
        return outputRelated;
    }

    public void setOutputRelated(long outputRelated) {
        this.outputRelated = outputRelated;
    }

    public StepState getState() {
        return state;
    }

    public void setState(StepState state) {
        this.state = state;
    }

    public SessionStepProperties getProperties() {
        return properties;
    }

    public void setProperties(SessionStepProperties properties) {
        this.properties = properties;
    }

    public OffsetDateTime getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(OffsetDateTime lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}