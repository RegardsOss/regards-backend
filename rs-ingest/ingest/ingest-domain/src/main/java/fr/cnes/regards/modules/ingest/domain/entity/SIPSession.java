/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.entity;

import java.time.OffsetDateTime;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

/**
 * Entities to group {@link SIPEntity}s. The {@link SIPSession#getLastActivationDate()}
 * is updated after each modification on a {@link SIPEntity} of the session.
 * @author Sébastien Binda
 */
@Entity
@Table(name = "t_sip_session", indexes = { @Index(name = "idx_sip_session", columnList = "id") })
public class SIPSession {

    /**
     * Session identifier (name)
     */
    @Id
    private String id;

    @NotNull
    private OffsetDateTime lastActivationDate;

    @Transient
    private long sipsCount = 0;

    @Transient
    private long indexedSipsCount = 0;

    @Transient
    private long storedSipsCount = 0;

    @Transient
    private long generatedSipsCount = 0;

    @Transient
    private long errorSipsCount = 0;

    @Transient
    private long submissionErrorCount = 0;

    @Transient
    private long deletedSipsCount = 0;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getSipsCount() {
        return sipsCount;
    }

    public void setSipsCount(long sipsCount) {
        this.sipsCount = sipsCount;
    }

    public long getIndexedSipsCount() {
        return indexedSipsCount;
    }

    public void setIndexedSipsCount(long indexedSipsCount) {
        this.indexedSipsCount = indexedSipsCount;
    }

    public long getStoredSipsCount() {
        return storedSipsCount;
    }

    public void setStoredSipsCount(long storedSipsCount) {
        this.storedSipsCount = storedSipsCount;
    }

    public long getGeneratedSipsCount() {
        return generatedSipsCount;
    }

    public void setGeneratedSipsCount(long generatedSipsCount) {
        this.generatedSipsCount = generatedSipsCount;
    }

    public long getErrorSipsCount() {
        return errorSipsCount;
    }

    public void setErrorSipsCount(long errorSipsCount) {
        this.errorSipsCount = errorSipsCount;
    }

    public long getDeletedSipsCount() {
        return deletedSipsCount;
    }

    public void setDeletedSipsCount(long deletedSipsCount) {
        this.deletedSipsCount = deletedSipsCount;
    }

    public OffsetDateTime getLastActivationDate() {
        return lastActivationDate;
    }

    public void setLastActivationDate(OffsetDateTime lastActivationDate) {
        this.lastActivationDate = lastActivationDate;
    }

    public long getSubmissionErrorCount() {
        return submissionErrorCount;
    }

    public void setSubmissionErrorCount(long submissionErrorCount) {
        this.submissionErrorCount = submissionErrorCount;
    }
}
