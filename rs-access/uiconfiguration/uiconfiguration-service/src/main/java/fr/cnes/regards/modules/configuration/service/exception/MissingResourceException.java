/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.service.exception;

/**
 * Runtime exception thrown when a required Spring @Resource is null.
 * @author Xavier-Alexandre Brochard
 */
public class MissingResourceException extends RuntimeException {

    /**
     * Main exception message
     */
    private static final String MESSAGE = "Error reading layout default configuration file. Null resource or inexistent.";

    /**
     * Default constructor
     */
    public MissingResourceException() {
        super();
    }

    /**
     * @param pCause
     * @param pEnableSuppression
     * @param pWritableStackTrace
     */
    public MissingResourceException(Throwable pCause, boolean pEnableSuppression, boolean pWritableStackTrace) {
        super(MESSAGE, pCause, pEnableSuppression, pWritableStackTrace);
    }

    /**
     * @param pCause
     */
    public MissingResourceException(Throwable pCause) {
        super(MESSAGE, pCause);
    }

}
