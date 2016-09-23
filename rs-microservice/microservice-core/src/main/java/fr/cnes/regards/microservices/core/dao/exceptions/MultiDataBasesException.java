/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.dao.exceptions;

public class MultiDataBasesException extends RuntimeException {

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 7382111289929689769L;

    public MultiDataBasesException(String pMessage, Throwable pCause, boolean pEnableSuppression,
            boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

    public MultiDataBasesException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    public MultiDataBasesException(String pMessage) {
        super(pMessage);
    }

    public MultiDataBasesException(Throwable pCause) {
        super(pCause);
    }

}
