/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.exception;

/**
 * @author svissier
 *
 */
public class RabbitMQVhostException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RabbitMQVhostException(String pMessage) {
        super(pMessage);
    }
}
