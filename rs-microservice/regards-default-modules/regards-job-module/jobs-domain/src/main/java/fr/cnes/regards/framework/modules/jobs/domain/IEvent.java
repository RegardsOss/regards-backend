/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.jobs.domain;

/**
 * 
 * @author Léo Mieulet
 *
 */
@Deprecated
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
