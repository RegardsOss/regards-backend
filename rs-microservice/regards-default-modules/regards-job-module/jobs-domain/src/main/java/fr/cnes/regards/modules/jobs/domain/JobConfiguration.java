/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;

import javax.persistence.Convert;

import fr.cnes.regards.modules.jobs.domain.converters.PathConverter;

/**
 * @author Léo Mieulet
 *
 */
public class JobConfiguration {

    /**
     * Job priority
     */
    private int priority;

    /**
     * Job expiration time
     */
    private LocalDateTime expirationDate;

    /**
     * Job workspace (nullable)
     */
    @Convert(converter = PathConverter.class)
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
     * Store some information about the job
     */
    private StatusInfo statusInfo;

    /**
     * The class to run (shall extends IJob) Example: fr.cnes.regards.modules.MyCustomJob
     */
    private String className;

    /**
     * Default constructor
     */
    public JobConfiguration() {
        super();
        statusInfo = new StatusInfo();
        statusInfo.setStartDate(LocalDateTime.now());
        statusInfo.setPercentCompleted(0);
        statusInfo.setJobStatus(JobStatus.QUEUED);
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
     * @param pExpirationDate
     *            the expirationDate to set
     */
    public void setExpirationDate(final LocalDateTime pExpirationDate) {
        expirationDate = pExpirationDate;
    }

    /**
     * @return the job class to execute
     */
    public String getClassName() {
        return className;
    }

    /**
     * @return the expirationDate
     */
    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    /**
     * @param pPriority
     *            the priority to set
     */
    public void setPriority(final int pPriority) {
        priority = pPriority;
    }

    /**
     * @param pWorkspace
     *            the workspace to set
     */
    public void setWorkspace(final Path pWorkspace) {
        workspace = pWorkspace;
    }

    /**
     * @param pParameters
     *            the parameters to set
     */
    public void setParameters(final JobParameters pParameters) {
        parameters = pParameters;
    }

    /**
     * @param pOwner
     *            the owner to set
     */
    public void setOwner(final String pOwner) {
        owner = pOwner;
    }

    /**
     * @param pStatusInfo
     *            the statusInfo to set
     */
    public void setStatusInfo(final StatusInfo pStatusInfo) {
        statusInfo = pStatusInfo;
    }

    /**
     * @param pClassName
     *            the className to set
     */
    public void setClassName(final String pClassName) {
        className = pClassName;
    }

}
