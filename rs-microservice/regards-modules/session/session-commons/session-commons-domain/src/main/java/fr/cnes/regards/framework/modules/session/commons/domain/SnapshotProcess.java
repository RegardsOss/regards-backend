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
package fr.cnes.regards.framework.modules.session.commons.domain;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

import java.util.Objects;
import javax.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.validation.constraints.NotNull;

/**
 * Process used to create or update {@link SessionStep}s from StepPropertyUpdateEventRequests
 *
 * @author Iliana Ghazali
 **/
@Entity
@Table(name = "t_snapshot_process")
public class SnapshotProcess {

    /**
     * Name of the source
     */
    @Id
    @Column(name = "source")
    @NotNull
    private String source;

    /**
     * Last date since when the SessionSteps were updated with StepPropertyUpdateEventRequests
     */
    @Column(name = "last_update_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime lastUpdateDate;

    /**
     * If there is an ongoing update on {@link SessionStep}
     */
    @Column(name = "job_id")
    private UUID jobId;

    public SnapshotProcess(String source, OffsetDateTime lastUpdateDate, UUID jobId) {
        this.source = source;
        this.lastUpdateDate = lastUpdateDate;
        this.jobId = jobId;
    }

    public SnapshotProcess() {
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(OffsetDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SnapshotProcess that = (SnapshotProcess) o;
        return source.equals(that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source);
    }

    @Override
    public String toString() {
        return "SnapshotProcess{" + "source='" + source + '\'' + ", lastUpdateDate=" + lastUpdateDate + ", jobId="
                + jobId + '}';
    }
}
