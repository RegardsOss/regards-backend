/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

/**
 * @author svissier
 *
 */
public class CleaningRabbitMQVhostException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public CleaningRabbitMQVhostException(String pMsg) {
        super(pMsg);
    }
}
