package fr.cnes.regards.framework.modules.session.sessioncommons.domain;

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
 * @author Iliana Ghazali
 **/
@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "t_session_step")
public class SessionStep {

    @Id
    @SequenceGenerator(name = "sessionSequence", initialValue = 1, sequenceName = "seq_session")
    @GeneratedValue(generator = "sessionSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "step_id")
    @Unique
    private String stepId;

    @Column(name = "source")
    @NotNull
    private String source;

    @Column(name = "session")
    @NotNull
    private String session;

    @Column(name = "type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepTypeEnum type;

    @Column(name = "in")
    @NotNull
    private Long in = 0L;

    @Column(name = "out")
    @NotNull
    private Long out = 0L;

    @Embedded
    @NotNull
    private StepState state;

    @Column(name = "properties", columnDefinition = "jsonb")
    @Type(type = "jsonb")
    private SessionStepProperties properties;

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

    public Long getIn() {
        return in;
    }

    public void setIn(Long in) {
        this.in = in;
    }

    public Long getOut() {
        return out;
    }

    public void setOut(Long out) {
        this.out = out;
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
