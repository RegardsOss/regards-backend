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
        status = JobStatus.PENDING;
    }

    /**
     * @param pDescription
     * @param pEstimatedCompletion
     * @param pExpirationDate
     * @param pPriority
     * @param pWorkspace
     * @param pParameters
     * @param pOwner
     */
    public JobConfiguration(String pDescription, JobParameters pParameters, String pClassName,
            LocalDateTime pEstimatedCompletion, LocalDateTime pExpirationDate, int pPriority, Path pWorkspace,
            String pOwner) {
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
     * @return
     */
    public String getOwner() {
        // TODO Auto-generated method stub
        return owner;
    }

    /**
     * @return
     */
    public Path getWorkspace() {
        // TODO Auto-generated method stub
        return workspace;
    }

    /**
     * @return
     */
    public int getPriority() {
        // TODO Auto-generated method stub
        return priority;
    }

    /**
     * @param pDescription
     *            the description to set
     */
    public void setDescription(String pDescription) {
        description = pDescription;
    }

    /**
     * @param pEstimatedCompletion
     *            the estimatedCompletion to set
     */
    public void setEstimatedCompletion(LocalDateTime pEstimatedCompletion) {
        estimatedCompletion = pEstimatedCompletion;
    }

    /**
     * @param pExpirationDate
     *            the expirationDate to set
     */
    public void setExpirationDate(LocalDateTime pExpirationDate) {
        expirationDate = pExpirationDate;
    }

    /**
     * @param pNextPoll
     *            the nextPoll to set
     */
    public void setNextPoll(LocalDateTime pNextPoll) {
        nextPoll = pNextPoll;
    }

    /**
     * @param pPercentCompleted
     *            the percentCompleted to set
     */
    public void setPercentCompleted(int pPercentCompleted) {
        percentCompleted = pPercentCompleted;
    }

    /**
     * @param pStartDate
     *            the startDate to set
     */
    public void setStartDate(LocalDateTime pStartDate) {
        startDate = pStartDate;
    }

    /**
     * @param pStatus
     *            the status to set
     */
    public void setStatus(JobStatus pStatus) {
        status = pStatus;
    }

    /**
     * @param pStopDate
     *            the stopDate to set
     */
    public void setStopDate(LocalDateTime pStopDate) {
        stopDate = pStopDate;
    }

    /**
     * @return
     */
    public String getClassName() {
        return className;
    }

}
