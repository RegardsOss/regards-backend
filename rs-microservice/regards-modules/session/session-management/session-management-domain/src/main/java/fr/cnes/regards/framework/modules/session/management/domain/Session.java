/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.session.management.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import java.time.OffsetDateTime;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

/**
 * A session is an aggregation of {@link SessionStep}
 *
 * @author Iliana Ghazali
 */
@Entity
@Table(name = "t_session")
public class Session {

    /**
     * Id of the SessionStep
     */
    @Id
    @SequenceGenerator(name = "sessionSequence", initialValue = 1, sequenceName = "seq_session")
    @GeneratedValue(generator = "sessionSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Name of the source
     */
    @Column(name = "source")
    @NotNull
    private String source;

    /**
     * Name of the session
     */
    @Column(name = "name")
    @NotNull
    private String name;

    /**
     * Session creation date
     */
    @Column(name = "creation_date")
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    /**
     * Date when session was last updated
     */
    @Column(name = "last_update_date")
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    /**
     * Set of session steps associated to this session
     */
    @Valid
    @Column(name = "steps")
    @NotNull(message = "At least one session step is required")
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "id", foreignKey = @ForeignKey(name = "fk_session_step_aggregation"))
    private Set<SessionStep> steps;

    /**
     * If session is running, ie, if one of SessionStep is running
     */
    @Column(name = "running")
    @NotNull
    private boolean running = false;

    /**
     * If session is in error, ie, if one of SessionStep is in error state
     */
    @Column(name = "error")
    @NotNull
    private boolean error = false;

    /**
     * If session is waiting, ie, if one of SessionStep is in waiting state
     */
    @Column(name = "waiting")
    @NotNull
    private boolean waiting = false;

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(OffsetDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public Set<SessionStep> getSteps() {
        return steps;
    }

    public void setSteps(Set<SessionStep> steps) {
        this.steps = steps;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}