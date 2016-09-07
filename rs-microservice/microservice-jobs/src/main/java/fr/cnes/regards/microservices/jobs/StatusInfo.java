package fr.cnes.regards.microservices.jobs;

import java.time.LocalDateTime;

public class StatusInfo {

    private String description;

    private LocalDateTime estimatedCompletion;

    private LocalDateTime expirationDate;

    private final JobId jobId;

    private LocalDateTime nextPoll;

    private int percentCompleted;

    private LocalDateTime StartDate;

    private JobStatus status;

    private LocalDateTime stopDate;

    public StatusInfo() {
        this.jobId = new JobId();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public LocalDateTime getEstimatedCompletion() {
        return estimatedCompletion;
    }

    public void setEstimatedCompletion(LocalDateTime pEstimatedCompletion) {
        estimatedCompletion = pEstimatedCompletion;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime pExpirationDate) {
        expirationDate = pExpirationDate;
    }

    public LocalDateTime getNextPoll() {
        return nextPoll;
    }

    public void setNextPoll(LocalDateTime pNextPoll) {
        nextPoll = pNextPoll;
    }

    public int getPercentCompleted() {
        return percentCompleted;
    }

    public void setPercentCompleted(int pPercentCompleted) {
        percentCompleted = pPercentCompleted;
    }

    public LocalDateTime getStartDate() {
        return StartDate;
    }

    public void setStartDate(LocalDateTime pStartDate) {
        StartDate = pStartDate;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus pStatus) {
        status = pStatus;
    }

    public LocalDateTime getStopDate() {
        return stopDate;
    }

    public void setStopDate(LocalDateTime pStopDate) {
        stopDate = pStopDate;
    }

    public JobId getJobId() {
        return jobId;
    }

}
