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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.framework.jpa.validator.PastOrNow;

/**
 * Store job status
 *
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@Entity
@Table(name = "t_job_status_info")
@SequenceGenerator(name = "statusInfoSequence", initialValue = 1, sequenceName = "seq_job_status_info")
public class StatusInfo implements IIdentifiable<Long> {

    /**
     * Job StatusInfo id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statusInfoSequence")
    @Column(name = "id")
    private Long id;

    /**
     * the job status
     */
    @Column(name = "status", length = 16)
    @Enumerated(value = EnumType.STRING)
    private JobStatus status;

    /**
     * Job StatusInfo description
     */
    @Column(name = "description")
    private String description;

    /**
     * Job StatusInfo estimated date to job completion
     */
    @Column(name = "estimatedCompletion")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime estimatedCompletion;

    /**
     * Job StatusInfo specify the date when the job should be expired
     */
    @Column(name = "expirationDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime expirationDate;

    /**
     * the job advancement
     */
    @Column(name = "percentCompleted")
    private int percentCompleted;

    /**
     * the job creation date
     */
    @PastOrNow
    @Column(name = "startDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime startDate;

    /**
     * the job end date
     */
    @Column(name = "stopDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime stopDate;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    public OffsetDateTime getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(final OffsetDateTime pEstimatedCompletion) {
        estimatedCompletion = pEstimatedCompletion;
    }

    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(final OffsetDateTime pExpirationDate) {
        expirationDate = pExpirationDate;
    }

    public int getPercentCompleted() {
        return percentCompleted;
    }

    public void setPercentCompleted(final int pPercentCompleted) {
        percentCompleted = pPercentCompleted;
    }

    public OffsetDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(final OffsetDateTime pStartDate) {
        startDate = pStartDate;
    }

    public JobStatus getJobStatus() {
        return status;
    }

    public void setJobStatus(final JobStatus pStatus) {
        status = pStatus;
    }

    public OffsetDateTime getStopDate() {
        return stopDate;
    }

    public void setStopDate(final OffsetDateTime pStopDate) {
        stopDate = pStopDate;
    }

    @Override
    public Long getId() {
        return id;
    }
}
