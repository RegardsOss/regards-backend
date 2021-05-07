package fr.cnes.regards.framework.modules.session.agent.domain.update;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

/**
 * Entity created after receiving a
 * {@link StepPropertyUpdateRequestEvent}.
 * <p>
 * {@link StepPropertyUpdateRequest}s will then be used to create {@link SessionStep}s, they are an aggregation of
 * these steps.
 *
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
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime date;

    @Column(name = "type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepPropertyEventTypeEnum type;

    @Embedded
    @NotNull
    private StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(
                    name = "gen_step_id",
                    referencedColumnName = "step_id"),
            @JoinColumn(
                    name = "gen_source",
                    referencedColumnName = "source"),
            @JoinColumn(
                    name = "gen_session",
                    referencedColumnName = "session")
    })
    private SessionStep sessionStep;

    public StepPropertyUpdateRequest(@NotNull String stepId, @NotNull String source, @NotNull String session,
            @NotNull OffsetDateTime date, @NotNull StepPropertyEventTypeEnum type,
            @NotNull StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.date = date;
        this.type = type;
        this.stepPropertyUpdateRequestInfo = stepPropertyUpdateRequestInfo;
    }
    public StepPropertyUpdateRequest(){
    }

    public Long getId() {
        return id;
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

    public StepPropertyEventTypeEnum getType() {
        return type;
    }

    public void setType(StepPropertyEventTypeEnum type) {
        this.type = type;
    }

    public StepPropertyUpdateRequestInfo getStepPropertyInfo() {
        return stepPropertyUpdateRequestInfo;
    }

    public void setStepPropertyInfo(StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo) {
        this.stepPropertyUpdateRequestInfo = stepPropertyUpdateRequestInfo;
    }

    public SessionStep getSessionStep() {
        return sessionStep;
    }

    public void setSessionStep(SessionStep sessionStep) {
        this.sessionStep = sessionStep;
    }
}