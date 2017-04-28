/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.concurrent.BlockingQueue;

/**
 * @author Léo Mieulet
 */
public abstract class AbstractJob implements IJob {

    /**
     * Share a queue between the job and the JobHandler
     */
    private BlockingQueue<IEvent> queueEvent;

    /**
     * JobInfo id
     */
    private Long jobInfoId;

    /**
     * Store the tenantName
     */
    private String tenantName;

    /**
     * Job parameters
     */
    protected JobParameters parameters;

    private Path workspace;

    @Override
    public void setQueueEvent(final BlockingQueue<IEvent> pQueueEvent) {
        queueEvent = pQueueEvent;
    }

    /**
     * Send an event to the JobHandler
     *
     * @param pEventType
     *            the event type
     * @param pValue
     *            data related to the event
     * @throws InterruptedException
     *             If interrupted while waiting
     */
    protected void sendEvent(final EventType pEventType, final Object pValue) throws InterruptedException {
        queueEvent.put(new Event(pEventType, pValue, jobInfoId, tenantName));
    }

    /**
     * @param pEventType
     *            the event type
     * @throws InterruptedException
     *             If interrupted while waiting
     */
    protected void sendEvent(final EventType pEventType) throws InterruptedException {
        queueEvent.put(new Event(pEventType, null, jobInfoId, tenantName));
    }

    /**
     * When the JobHandler creates this job, it saves the jobId
     *
     * @param pJobInfoId
     */
    @Override
    public void setJobInfoId(final Long pJobInfoId) {
        jobInfoId = pJobInfoId;
    }

    /**
     * @return the parameters
     */
    public JobParameters getParameters() {
        return parameters;
    }

    /**
     * @param pTenantName
     *            the tenantName to set
     */
    @Override
    public void setTenantName(final String pTenantName) {
        tenantName = pTenantName;
    }

    @Override
    public void setWorkspace(Path pWorkspace) {
        workspace = pWorkspace;
    }

    public Path getWorkspace() {
        return workspace;
    }
}
