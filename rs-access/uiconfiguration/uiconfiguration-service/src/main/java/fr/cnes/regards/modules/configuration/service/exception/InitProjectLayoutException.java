/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service.exception;

/**
 * Runtime exception thrown when an error occurs during project layout intialization.
 * @author Xavier-Alexandre Brochard
 */
public class InitProjectLayoutException extends RuntimeException {

    /**
     * Main exception message
     */
    private static final String MESSAGE = "Error reading layout default configuration file";

    /**
     * Default constructor
     */
    public InitProjectLayoutException() {
        super();
    }

    /**
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public InitProjectLayoutException(Throwable pCause, boolean pEnableSuppression, boolean pWritableStackTrace) {
        super(MESSAGE, pCause, pEnableSuppression, pWritableStackTrace);
    }

    /**
     * @param pCause
     */
    public InitProjectLayoutException(Throwable pCause) {
        super(MESSAGE, pCause);
    }

}
