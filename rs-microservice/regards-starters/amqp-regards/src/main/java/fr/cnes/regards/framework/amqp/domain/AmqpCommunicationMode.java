/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

/**
 *
 * Enum containing each communication mode possible in REGARDS
 *
 * @author svissier
 *
 */
public enum AmqpCommunicationMode {
    /**
     *
     * It means the exchange that is associated with it is a direct exchange and determine how the system names the
     * exchanges/queues/bindings
     *
     */
    ONE_TO_ONE,
    /**
     * It means the exchange that is associated with it is a fanout exchange and determine how the system names the
     * exchanges/queues/bindings
     */
    ONE_TO_MANY;
}
