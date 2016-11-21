/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

/**
 * 
 * @author Léo Mieulet
 *
 */
public interface IEvent {

    public Object getData();

    /**
     * @return the event type
     */
    EventType getType();

    /**
     * @return the jobInfo id
     */
    Long getJobInfoId();

    /**
     * @return the tenantName
     */
    public String getTenantName();
}
