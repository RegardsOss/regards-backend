/**
 *
 */
package fr.cnes.regards.modules.jobs.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * @author lmieulet
 *
 */
public class JobConfiguration {

    /**
     * Job priority
     */
    private int priority;

    /**
     * Job description
     */
    private String description;

    /**
     * Job estimation time to complete
     */
    private LocalDateTime estimatedCompletion;

    /**
     * Job expiration time
     */
    private LocalDateTime expirationDate;

    /**
     * No idea.
     */
    private LocalDateTime nextPoll;

    /**
     * Job progress
     */
    private int percentCompleted;

    /**
     * Job start date
     */
    private LocalDateTime startDate;

    /**
     * Job end date
     */
    private LocalDateTime stopDate;

    /**
     * Job workspace (nullable)
     */
    private Path workspace;

    /**
     * Job parameters
     */
    private JobParameters parameters;

    /**
     * Either the job launched by the user or by the system
     */
    private String owner;

    /**
     * Job state
     */
    private JobStatus status;

    /**
     * Store some information about the job
     */
    private StatusInfo statusInfo;

    /**
     * The class to run (shall extends IJob) Example: fr.cnes.regards.modules.MyCustomJob
     */
    private String className;

    /**
     *
     */
    public JobConfiguration() {
        super();
        startDate = LocalDateTime.now();
        stopDate = null;
        percentCompleted = 0;
        status = JobStatus.QUEUED;
    }

    /**
     * @param pDescription
     *            Job description
     * @param pParameters
     *            job parameters
     * @param pClassName
     *            the job class to execute
     * @param pEstimatedCompletion
     *            estimated date to job completion
     * @param pExpirationDate
     *            specify the date when the job should be expired
     * @param pPriority
     *            the job priority
     * @param pWorkspace
     *            the job workspace
     * @param pOwner
     *            job owner
     */
    public JobConfiguration(final String pDescription, final JobParameters pParameters, final String pClassName,
            final LocalDateTime pEstimatedCompletion, final LocalDateTime pExpirationDate, final int pPriority,
            final Path pWorkspace, final String pOwner) {
        this();
        statusInfo = new StatusInfo();
        statusInfo.setDescription(pDescription);
        statusInfo.setEstimatedCompletion(pEstimatedCompletion);
        statusInfo.setExpirationDate(pExpirationDate);
        priority = pPriority;
        workspace = pWorkspace;
        parameters = pParameters;
        owner = pOwner;
        className = pClassName;
    }

    /**
     * @return create a StatusInfo object based on configuration stored in this class
     */
    public StatusInfo getStatusInfo() {
        return statusInfo;
    }

    /**
     * @return parameters
     */
    public JobParameters getParameters() {
        return parameters;
    }

    /**
     * @return the owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * @return the workspace
     */
    public Path getWorkspace() {
        return workspace;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @param pDescription
     *            the description to set
     */
    public void setDescription(final String pDescription) {
        description = pDescription;
    }

    /**
     * @param pEstimatedCompletion
     *            the estimatedCompletion to set
     */
    public void setEstimatedCompletion(final LocalDateTime pEstimatedCompletion) {
        estimatedCompletion = pEstimatedCompletion;
    }

    /**
     * @param pExpirationDate
     *            the expirationDate to set
     */
    public void setExpirationDate(final LocalDateTime pExpirationDate) {
        expirationDate = pExpirationDate;
    }

    /**
     * @param pNextPoll
     *            the nextPoll to set
     */
    public void setNextPoll(final LocalDateTime pNextPoll) {
        nextPoll = pNextPoll;
    }

    /**
     * @param pPercentCompleted
     *            the percentCompleted to set
     */
    public void setPercentCompleted(final int pPercentCompleted) {
        percentCompleted = pPercentCompleted;
    }

    /**
     * @param pStartDate
     *            the startDate to set
     */
    public void setStartDate(final LocalDateTime pStartDate) {
        startDate = pStartDate;
    }

    /**
     * @param pStatus
     *            the status to set
     */
    public void setStatus(final JobStatus pStatus) {
        status = pStatus;
    }

    /**
     * @param pStopDate
     *            the stopDate to set
     */
    public void setStopDate(final LocalDateTime pStopDate) {
        stopDate = pStopDate;
    }

    /**
     * @return the job class to execute
     */
    public String getClassName() {
        return className;
    }

}
