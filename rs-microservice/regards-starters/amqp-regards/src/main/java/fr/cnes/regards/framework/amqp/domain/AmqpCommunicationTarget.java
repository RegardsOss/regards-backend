/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.domain;

/**
 * Constant to designed if the target of published messages is internal to the microservice type or not
 *
 * @author svissier
 *
 */
public enum AmqpCommunicationTarget {
    /**
     * means that the target of the published message is internal
     */
    INTERNAL,
    /**
     * means that the target of the published message is external
     */
    EXTERNAL;
}
