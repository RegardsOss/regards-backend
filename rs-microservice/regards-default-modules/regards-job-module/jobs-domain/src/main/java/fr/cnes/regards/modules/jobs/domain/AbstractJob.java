/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

import java.util.concurrent.BlockingQueue;

/**
 *
 */
public abstract class AbstractJob implements IJob {

    private BlockingQueue<IEvent> queueEvent;

    private Long jobInfoId;

    private JobParameters parameters;

    @Override
    public void setQueueEvent(final BlockingQueue<IEvent> pQueueEvent) {
        queueEvent = pQueueEvent;
    }

    /**
     * @param pEventType
     * @param pValue
     * @throws InterruptedException
     */
    protected void sendEvent(final EventType pEventType, final Object pValue) throws InterruptedException {
        queueEvent.put(new Event(pEventType, pValue, jobInfoId));
    }

    /**
     * @param pEventType
     * @param pValue
     * @throws InterruptedException
     */
    protected void sendEvent(final EventType pEventType) throws InterruptedException {
        queueEvent.put(new Event(pEventType, null, jobInfoId));
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

}
