/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.validator.PastOrNow;

/**
 * Store job status
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@Entity(name = "T_JOB_STATUS_INFO")
@SequenceGenerator(name = "statusInfoSequence", initialValue = 1, sequenceName = "SEQ_JOB_STATUS_INFO")
public class StatusInfo implements IIdentifiable<Long> {

    /**
     * Job StatusInfo id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statusInfoSequence")
    @Column(name = "id")
    private Long id;

    /**
     * Job StatusInfo description
     */
    @Column(name = "description")
    private String description;

    /**
     * Job StatusInfo estimated date to job completion
     */
    @Column(name = "estimatedCompletion")
    private LocalDateTime estimatedCompletion;

    /**
     * Job StatusInfo specify the date when the job should be expired
     */
    @Column(name = "expirationDate")
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
    private LocalDateTime startDate;

    /**
     * the job status
     */
    @Column(name = "status")
    @Enumerated(value=EnumType.ORDINAL)
    private JobStatus status;

    /**
     * the job end date
     */
    @Column(name = "stopDate")
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
