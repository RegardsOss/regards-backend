/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.domain.job;

import java.time.OffsetDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;

/**
 * This class allows to keep track of a job for monitoring purpose.
 *
 * @author Marc Sordi
 *
 */
@MappedSuperclass
public abstract class JobReport {

    /**
     * The active job id. If job is finished, this id should be null
     */
    @Column(name = "job_id")
    protected UUID jobId;

    /**
     * If jobId exists, allows to load a transient {@link JobInfo}
     */
    @Transient
    protected JobInfo jobInfo;

    /**
     * the job schedule date
     */
    @Column(name = "schedule_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime scheduleDate;

    /**
     * the job creation date
     */
    @Column(name = "start_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime startDate;

    /**
     * the job end date
     */
    @Column(name = "stop_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime stopDate;

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public JobInfo getJobInfo() {
        return jobInfo;
    }

    public void setJobInfo(JobInfo jobInfo) {
        this.jobInfo = jobInfo;
    }

    public OffsetDateTime getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(OffsetDateTime scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;
    }

    public OffsetDateTime getStopDate() {
        return stopDate;
    }

    public void setStopDate(OffsetDateTime stopDate) {
        this.stopDate = stopDate;
    }
}
