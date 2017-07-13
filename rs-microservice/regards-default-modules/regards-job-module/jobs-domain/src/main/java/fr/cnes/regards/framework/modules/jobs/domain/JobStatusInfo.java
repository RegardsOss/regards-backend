/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Type;

import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;

/**
 * Store job status
 * @author Léo Mieulet
 * @author Christophe Mertz
 */
@Embeddable
public class JobStatusInfo {

    /**
     * Job status
     */
    @Column(name = "status", length = 16)
    @Enumerated(value = EnumType.STRING)
    private JobStatus status;

    /**
     * Date of current status update
     */
    @Column(name = "statusDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime statusDate;

    /**
     * Estimated date to job completion
     */
    @Column(name = "estimatedCompletion")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime estimatedCompletion;

    /**
     * the job advancement
     */
    @Column(name = "percentCompleted")
    private int percentCompleted;

    /**
     * the job creation date
     */
    @Column(name = "startDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime startDate;

    /**
     * the job end date
     */
    @Column(name = "stopDate")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    private OffsetDateTime stopDate;

    /**
     * In case of error, contains the stack trace
     */
    @Column(name = "stacktrace")
    @Type(type = "text")
    private String stackTrace;

    public JobStatusInfo() {
    }

    public OffsetDateTime getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(final OffsetDateTime pEstimatedCompletion) {
        estimatedCompletion = pEstimatedCompletion;
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

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(final JobStatus pStatus) {
        status = pStatus;
        statusDate = OffsetDateTime.now();
    }

    public OffsetDateTime getStopDate() {
        return stopDate;
    }

    public void setStopDate(final OffsetDateTime pStopDate) {
        stopDate = pStopDate;
    }

    public OffsetDateTime getStatusDate() {
        return statusDate;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }
}
