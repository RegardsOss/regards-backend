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
package fr.cnes.regards.framework.modules.session.manager.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.framework.modules.session.commons.domain.SessionStep;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A session is an aggregation of {@link SessionStep}
 *
 * @author Iliana Ghazali
 */
@Entity
@Table(name = "t_session_manager")
public class Session {

    /**
     * Id of the SessionStep
     */
    @Id
    @SequenceGenerator(name = "sessionManagerSequence", initialValue = 1, sequenceName = "seq_session_manager")
    @GeneratedValue(generator = "sessionManagerSequence", strategy = GenerationType.SEQUENCE)
    private Long id;

    /**
     * Name of the session
     */
    @Column(name = "name")
    @NotNull
    private String name;

    /**
     * Name of the related source
     */
    @Column(name = "source")
    @NotNull
    private String source;

    /**
     * Session creation date
     */
    @Column(name = "creation_date")
    @NotNull
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate = OffsetDateTime.now();

    /**
     * Date when session was last updated
     */
    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    /**
     * Set of session steps associated to this session
     */
    @Valid
    @NotNull
    @Column(name = "steps", columnDefinition = "jsonb")
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE,
                                    value = "fr.cnes.regards.framework.modules.session.commons.domain.SessionStep") })
    private Set<SessionStep> steps = new HashSet<>();

    @Embedded
    @NotNull
    private ManagerState managerState = new ManagerState();

    public Session(@NotNull String source, @NotNull String name) {
        this.source = source;
        this.name = name;
    }

    public Session() {
    }

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

    public void setSteps(Set<SessionStep> pSteps) {
        // This method is used to prevent the override of the set that Hibernate is tracking
        this.steps.clear();
        if (pSteps != null) {
            this.steps.addAll(pSteps);
        }
    }

    public ManagerState getManagerState() {
        return managerState;
    }

    public void setManagerState(ManagerState managerState) {
        this.managerState = managerState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Session session = (Session) o;
        return name.equals(session.name) && source.equals(session.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, source);
    }

    @Override
    public String toString() {
        return "Session{"
               + "id="
               + id
               + ", name='"
               + name
               + '\''
               + ", source='"
               + source
               + '\''
               + ", creationDate="
               + creationDate
               + ", lastUpdateDate="
               + lastUpdateDate
               + ", steps="
               + steps
               + ", managerState="
               + managerState
               + '}';
    }
}