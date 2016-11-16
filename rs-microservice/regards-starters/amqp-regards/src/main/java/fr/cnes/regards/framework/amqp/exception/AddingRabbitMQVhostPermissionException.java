/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.exception;

/**
 * @author svissier
 *
 */
public class AddingRabbitMQVhostPermissionException extends RabbitMQVhostException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AddingRabbitMQVhostPermissionException(String pMessage) {
        super(pMessage);
    }

}
