/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jobs.domain;

public interface IEvent {

    public Object getData();

    /**
     * @return
     */
    EventType getType();

    /**
     * @return
     */
    Long getJobInfoId();
}
