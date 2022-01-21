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
package fr.cnes.regards.framework.modules.session.agent.domain.update;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import javax.persistence.*;
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

    @Column(name = "creation_date")
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    @Column(name = "type")
    @NotNull
    @Enumerated(value = EnumType.STRING)
    private StepPropertyEventTypeEnum type;

    @Embedded
    @NotNull
    private StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({ @JoinColumn(name = "gen_step_id", referencedColumnName = "step_id"),
            @JoinColumn(name = "gen_source", referencedColumnName = "source"),
            @JoinColumn(name = "gen_session", referencedColumnName = "session") })
    private SessionStep sessionStep;

    @Column(name = "registration_date")
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime registrationDate;

    public StepPropertyUpdateRequest(@NotNull String stepId, @NotNull String source, @NotNull String session,
            @NotNull OffsetDateTime creationDate, @NotNull StepPropertyEventTypeEnum type,
            @NotNull StepPropertyUpdateRequestInfo stepPropertyUpdateRequestInfo) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.creationDate = creationDate;
        this.type = type;
        this.stepPropertyUpdateRequestInfo = stepPropertyUpdateRequestInfo;
        this.registrationDate = OffsetDateTime.now();
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

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
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

    public OffsetDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(OffsetDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public String toString() {
        return "StepPropertyUpdateRequest{" + "id=" + id + ", stepId='" + stepId + '\'' + ", source='" + source + '\''
                + ", session='" + session + '\'' + ", creationDate=" + creationDate + ", type=" + type
                + ", stepPropertyUpdateRequestInfo=" + stepPropertyUpdateRequestInfo + ", sessionStep=" + sessionStep
                + ", registrationDate=" + registrationDate + '}';
    }
}