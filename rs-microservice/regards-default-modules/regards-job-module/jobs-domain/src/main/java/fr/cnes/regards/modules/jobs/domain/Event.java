/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

/**
 * 
 * @author Léo Mieulet
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
     * Store the tenantName
     */
    private final String tenantName;

    /**
     * @param pEventType
     *            the event type
     * @param pData
     *            to store some data
     * @param pJobInfoId
     *            the jobInfo id
     * @param pTenantName
     *            the tenant name
     */
    public Event(final EventType pEventType, final Object pData, final Long pJobInfoId, final String pTenantName) {
        super();
        eventType = pEventType;
        data = pData;
        jobId = pJobInfoId;
        tenantName = pTenantName;
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

    @Override
    public String getTenantName() {
        return tenantName;
    }
}
