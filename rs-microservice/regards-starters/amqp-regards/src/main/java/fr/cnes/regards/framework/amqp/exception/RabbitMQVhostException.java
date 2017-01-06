/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.exception;

/**
 *
 * exceptions that reflect a problem while interacting with the message broker. Mainly while adding a virtual
 * host(tenant)
 *
 * FIXME: should be runtimeException?
 *
 * @author svissier
 *
 */
public class RabbitMQVhostException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public RabbitMQVhostException(String pMessage) {
        super(pMessage);
    }
}
