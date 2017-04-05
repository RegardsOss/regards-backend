/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.event;

/**
 *
 * Available values for event target.<br/>
 * If {@link Target#ALL}, all instances of all microservice types can poll or subscribe to this event. <br/>
 * If {@link Target#MICROSERVICE}, only instances of the same microservice can.<br/>
 * if {@link Target#INSTANCE}, only current instance can. Similar to a local JAVA notification.
 *
 * @author Marc Sordi
 *
 */
public enum Target {

    /**
     * Available values for event target
     */
    ALL, MICROSERVICE, INSTANCE;
}
