/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.exception;

/**
 * @author Marc Sordi
 *
 */
@SuppressWarnings("serial")
public class RemovingRabbitMQVhostException extends RabbitMQVhostException {

    public RemovingRabbitMQVhostException(String pMessage) {
        super(pMessage);
    }

}
