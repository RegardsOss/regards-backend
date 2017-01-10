/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

/**
 *
 * Event you can poll and acknowledge
 *
 * @author Marc Sordi
 *
 */
public interface IPollableEvent extends IEvent {

    /**
     * Define the worker mode
     *
     * @return {@link WorkerMode}
     */
    WorkerMode withWorkerMode();
}
