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
package fr.cnes.regards.modules.sessionmanager.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

/**
 * Session gathers information about data submission provided by unknown actors
 * @author Léo Mieulet
 */
@Entity
@TypeDefs({@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)})
@Table(name = "t_session",
        indexes = {
                @Index(name = "idx_session_name", columnList = "name,source"),
                @Index(name = "idx_session_state", columnList = "state"),
                @Index(name = "idx_session_creation_date", columnList = "creation_date"),
                @Index(name = "idx_session_is_latest", columnList = "is_latest"),
                @Index(name = "idx_last_update_date", columnList = "last_update_date")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_session_source_name", columnNames = {"name", "source"})})
public class Session {

    /**
     * Default length of string fields
     */
    private static final int STRING_FIELDS_LENGTH = 128;
    /**
     * State field length (matching enum size)
     */
    private static final int STATE_FIELD_LENGTH = 16;

    /**
     * Session Unique Identifier
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sessionSequence")
    @Column(name = "id")
    @SequenceGenerator(name = "sessionSequence", initialValue = 1, sequenceName = "seq_session")
    private Long id;

    /**
     * Session name
     */
    @Column(length = STRING_FIELDS_LENGTH, nullable = false)
    private String name;

    /**
     * Session source
     */
    @Column(length = STRING_FIELDS_LENGTH, nullable = false)
    private String source;

    /**
     * The date of creation of the Session
     */
    @Column(name = "creation_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime creationDate;

    /**
     * The last update date of the Session
     */
    @Column(name = "last_update_date", nullable = false)
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    /**
     * Is this session the latest from all sessions that shares the same source
     */
    @Column(name = "is_latest")
    private boolean isLatest;

    /**
     * Internal state of the session
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "state", length = STATE_FIELD_LENGTH, nullable = false)
    private SessionState state = SessionState.OK;

    /**
     * The session life cycle summary: each session actor can create and update an object (second map) here
     */
    @Column(columnDefinition = "jsonb", name = "life_cycle")
    @Type(type = "jsonb")
    private SessionLifeCycle lifeCycle;

    /**
     * Default constructor
     */
    public Session() {
    }

    /**
     * Constructor to setup a session
     * @param source
     * @param name
     */
    public Session(String source, String name) {
        this.name = name;
        this.source = source;
        this.creationDate = OffsetDateTime.now();
        this.lastUpdateDate = OffsetDateTime.now();
        this.isLatest = true;
        this.lifeCycle = new SessionLifeCycle();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
    }

    public Map<String, Map<String, Object>> getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(SessionLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public boolean isStepExisting(String step) {
        return lifeCycle.containsKey(step);
    }

    public boolean isStepPropertyExisting(String step, String property) {
        return this.isStepExisting(step) && lifeCycle.get(step).containsKey(property);
    }

    public Object getStepPropertyValue(String step, String property) {
        if (!isStepPropertyExisting(step, property)) {
            return null;
        }
        return lifeCycle.get(step).get(property);
    }

    public void setStepPropertyValue(String step, String property, Object value) {
        // Initialise the step level if not existing
        if (!isStepExisting(step)) {
            lifeCycle.put(step, new HashMap<>());
        }
        lifeCycle.get(step).put(property, value);
    }

    public boolean isLatest() {
        return isLatest;
    }

    public void setLatest(boolean latest) {
        isLatest = latest;
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

        if (name != null ? !name.equals(session.name) : session.name != null) {
            return false;
        }
        return source != null ? source.equals(session.source) : session.source == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (source != null ? source.hashCode() : 0);
        return result;
    }
}
