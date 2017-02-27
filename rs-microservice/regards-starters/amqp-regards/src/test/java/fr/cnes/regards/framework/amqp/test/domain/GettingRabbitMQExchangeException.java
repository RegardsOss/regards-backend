/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

/**
 * @author svissier
 *
 */
@SuppressWarnings("serial")
public class GettingRabbitMQExchangeException extends Exception {

    /**
     *
     */
    public GettingRabbitMQExchangeException() {

    }

    public GettingRabbitMQExchangeException(String pMessage) {
        super(pMessage);

    }

    public GettingRabbitMQExchangeException(Throwable pCause) {
        super(pCause);

    }

    public GettingRabbitMQExchangeException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);

    }

    public GettingRabbitMQExchangeException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);

    }

}
