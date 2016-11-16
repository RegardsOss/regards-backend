/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.domain;

/**
 * @author svissier
 *
 */
public class GettingRabbitMQExchangeException extends Exception {

    /**
     *
     */
    public GettingRabbitMQExchangeException() {

    }

    /**
     * @param pMessage
     */
    public GettingRabbitMQExchangeException(String pMessage) {
        super(pMessage);

    }

    /**
     * @param pCause
     */
    public GettingRabbitMQExchangeException(Throwable pCause) {
        super(pCause);

    }

    /**
     * @param pMessage
     * @param pCause
     */
    public GettingRabbitMQExchangeException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);

    }

    /**
     * @param pMessage
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public GettingRabbitMQExchangeException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);

    }

}
