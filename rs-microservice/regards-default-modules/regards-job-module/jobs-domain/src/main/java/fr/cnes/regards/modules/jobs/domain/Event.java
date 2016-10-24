/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

/**
 *
 */
public class Event implements IEvent {

    private final EventType eventType;

    private final Object data;

    private final Long jobId;

    /**
     * @param pEventType
     * @param pData
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
