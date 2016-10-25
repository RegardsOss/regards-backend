/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test;

/**
 * @author svissier
 *
 */
public class GettingRabbitMQQueueException extends Exception {

    /**
     *
     */
    public GettingRabbitMQQueueException() {
    }

    /**
     * @param pMessage
     */
    public GettingRabbitMQQueueException(String pMessage) {
        super(pMessage);
    }

    /**
     * @param pCause
     */
    public GettingRabbitMQQueueException(Throwable pCause) {
        super(pCause);
    }

    /**
     * @param pMessage
     * @param pCause
     */
    public GettingRabbitMQQueueException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    /**
     * @param pMessage
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public GettingRabbitMQQueueException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

}
