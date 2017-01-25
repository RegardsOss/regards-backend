/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

/**
 * Available worker mode for {@link IPollable}.<br/>
 * if {@link WorkerMode#SINGLE}, a <b>single worker</b> can handle the event.<br/>
 * if {@link WorkerMode#ALL}, <b>all workers</b> can handle this same event.<br/>
 * A
 *
 * @author Marc Sordi
 *
 */
public enum WorkerMode {

    /**
     * Available worker mode
     */
    SINGLE, ALL;
}
