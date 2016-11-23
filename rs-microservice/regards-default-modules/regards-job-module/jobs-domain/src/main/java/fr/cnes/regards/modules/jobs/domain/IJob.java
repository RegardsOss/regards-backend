/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Interface for all regards jobs
 * 
 * @author Léo Mieulet
 */
public interface IJob extends Runnable {

    
    /**
     * 
     * @return The job's priority
     */
    int getPriority();

    /**
     * 
     * @return The Job's results
     */
    List<Output> getResults();

    /**
     * 
     * @return The job's {@link StatusInfo}
     */
    StatusInfo getStatus();

    /**
     * 
     * @return the job has a result ?
     */
    boolean hasResult();

    /**
     * 
     * @return the job need a workspace ?
     */
    boolean needWorkspace();

    /**
     *
     * @param pPath
     *            set workspace path
     */
    void setWorkspace(Path pPath);

    /**
     * @param pQueueEvent
     *            setup the BlockingQueue (thread safe) into the job to communicate between the JobHandler and the
     *            running Job
     */
    void setQueueEvent(final BlockingQueue<IEvent> pQueueEvent);

    /**
     * @param pJobInfoId
     *            save the jobInfo id inside the job
     */
    void setJobInfoId(final Long pJobInfoId);

    /**
     * @param pParameters
     *            set job parameters
     *
     */
    void setParameters(JobParameters pParameters);

    /**
     * @param pTenantName
     *            set the tenant name
     */
    void setTenantName(String pTenantName);

}
