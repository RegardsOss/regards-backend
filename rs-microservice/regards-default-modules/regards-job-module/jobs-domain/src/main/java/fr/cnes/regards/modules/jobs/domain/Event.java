/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

/**
 *
 */
public class Event implements IEvent {

    /**
     * Store the event type
     */
    private final EventType eventType;

    /**
     * Store any data related to the event (nullable)
     */
    private final Object data;

    /**
     * Store the jobId
     */
    private final Long jobId;

    /**
     * @param pEventType
     *            the event type
     * @param pData
     *            to store some data
     * @param pJobId
     *            the jobInfo id
     */
    public Event(final EventType pEventType, final Object pData, final Long pJobId) {
        super();
        eventType = pEventType;
        data = pData;
        jobId = pJobId;
    }

    @Override
    public Object getData() {
        return data;
    }

    @Override
    public EventType getType() {
        return eventType;
    }

    @Override
    public Long getJobInfoId() {
        return jobId;
    }

}
