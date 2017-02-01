/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.time.LocalDateTime;

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
import fr.cnes.regards.framework.jpa.converters.LocalDateTimeAttributeConverter;
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
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime estimatedCompletion;

    /**
     * Job StatusInfo specify the date when the job should be expired
     */
    @Column(name = "expirationDate")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime expirationDate;

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
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime startDate;

    /**
     * the job end date
     */
    @Column(name = "stopDate")
    @Convert(converter = LocalDateTimeAttributeConverter.class)
    private LocalDateTime stopDate;

    public String getDescription() {
        return description;
    }

    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    public LocalDateTime getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(final LocalDateTime pEstimatedCompletion) {
        estimatedCompletion = pEstimatedCompletion;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(final LocalDateTime pExpirationDate) {
        expirationDate = pExpirationDate;
    }

    public int getPercentCompleted() {
        return percentCompleted;
    }

    public void setPercentCompleted(final int pPercentCompleted) {
        percentCompleted = pPercentCompleted;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(final LocalDateTime pStartDate) {
        startDate = pStartDate;
    }

    public JobStatus getJobStatus() {
        return status;
    }

    public void setJobStatus(final JobStatus pStatus) {
        status = pStatus;
    }

    public LocalDateTime getStopDate() {
        return stopDate;
    }

    public void setStopDate(final LocalDateTime pStopDate) {
        stopDate = pStopDate;
    }

    @Override
    public Long getId() {
        return id;
    }
}
