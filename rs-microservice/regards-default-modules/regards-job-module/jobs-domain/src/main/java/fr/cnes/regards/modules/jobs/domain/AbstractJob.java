/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.util.concurrent.BlockingQueue;

/**
 *
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
    private JobParameters parameters;

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
     */
    protected void sendEvent(final EventType pEventType, final Object pValue) throws InterruptedException {
        queueEvent.put(new Event(pEventType, pValue, jobInfoId, tenantName));
    }

    /**
     * @param pEventType
     *            the event type
     * @throws InterruptedException
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
     * @param pParameters
     */
    @Override
    public void setParameters(final JobParameters pParameters) {
        parameters = pParameters;
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
    public void setTenantName(final String pTenantName) {
        tenantName = pTenantName;
    }

}
