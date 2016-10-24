/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import fr.cnes.regards.modules.core.deserializer.LocalDateTimeDeserializer;
import fr.cnes.regards.modules.core.serializer.LocalDateTimeSerializer;
import fr.cnes.regards.modules.core.validation.PastOrNow;

/**
 * Store job status
 */
@Entity(name = "T_JOB_STATUS_INFO")
@SequenceGenerator(name = "statusInfoSequence", initialValue = 1, sequenceName = "SEQ_JOB_STATUS_INFO")
public class StatusInfo {

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
     * TODO
     */
    @Column(name = "nextPoll")
    private LocalDateTime nextPoll;

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

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getNextPoll() {
        return nextPoll;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setNextPoll(final LocalDateTime pNextPoll) {
        nextPoll = pNextPoll;
    }

    public int getPercentCompleted() {
        return percentCompleted;
    }

    public void setPercentCompleted(final int pPercentCompleted) {
        percentCompleted = pPercentCompleted;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getStartDate() {
        return startDate;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setStartDate(final LocalDateTime pStartDate) {
        startDate = pStartDate;
    }

    public JobStatus getJobStatus() {
        return status;
    }

    public void setJobStatus(final JobStatus pStatus) {
        status = pStatus;
    }

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getStopDate() {
        return stopDate;
    }

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    public void setStopDate(final LocalDateTime pStopDate) {
        stopDate = pStopDate;
    }
}
