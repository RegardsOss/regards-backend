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
package fr.cnes.regards.framework.modules.session.commons.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * A {@link SessionStep} represents a step in which data are added or processed. Currently 4 steps mainly exist to
 * process data during its life cycle (acquisition/referencing/storage/dissemination). They are created or updated
 * through step property events from the session agent. They are then sent to the session manager to be aggregated in
 * sessions and sources.
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_session_step")
@IdClass(SessionStepId.class)
public class SessionStep {

    /**
     * Step identifier
     */
    @Id
    @Column(name = "step_id")
    @NotNull
    private String stepId;

    /**
     * Name of the source
     */
    @Id
    @Column(name = "source")
    @NotNull
    private String source;

    /**
     * Name of the session
     */
    @Id
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
    @Type(JsonBinaryType.class)
    @NotNull
    private SessionStepProperties properties = new SessionStepProperties();

    /**
     * Most recent StepPropertyUpdateEventRequest
     */
    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    @NotNull
    private OffsetDateTime lastUpdateDate;

    /**
     * Date when the SessionStep is registered in the database after the reception SessionStepEvents. Only filled in
     * SessionManager
     */
    @Column(name = "registration_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime registrationDate;

    public SessionStep(@NotNull String stepId,
                       @NotNull String source,
                       @NotNull String session,
                       @NotNull StepTypeEnum type,
                       @NotNull StepState state) {
        this.stepId = stepId;
        this.source = source;
        this.session = session;
        this.type = type;
        this.state = state;
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

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public OffsetDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(OffsetDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SessionStep that = (SessionStep) o;
        return stepId.equals(that.stepId) && source.equals(that.source) && session.equals(that.session);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stepId, source, session);
    }

    @Override
    public String toString() {
        return "SessionStep{"
               + "stepId='"
               + stepId
               + '\''
               + ", source='"
               + source
               + '\''
               + ", session='"
               + session
               + '\''
               + ", type="
               + type
               + ", inputRelated="
               + inputRelated
               + ", outputRelated="
               + outputRelated
               + ", state="
               + state
               + ", properties="
               + properties
               + ", lastUpdateDate="
               + lastUpdateDate
               + ", registrationDate="
               + registrationDate
               + '}';
    }
}