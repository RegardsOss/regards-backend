/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Exception for plugin utils package
 *
 * @author msordi
 */
public class PluginUtilsException extends Exception {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsException.class);

    /**
     * serialVersionUID field.
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    private static final long serialVersionUID = 1L;

    /**
     *
     * Default constructor
     *
     * @param pMessage
     *            an error message
     * @since 1.0-SNAPSHOT
     */
    public PluginUtilsException(String pMessage) {
        super(pMessage);
        LOGGER.error(pMessage);
    }

    /**
     *
     * Constructor
     *
     * @param pMessage
     *            an error message
     * @param pCause
     *            the exception
     * @since 1.0-SNAPSHOT
     */
    public PluginUtilsException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
        LOGGER.error(pMessage, pCause);
    }

    /**
     *
     * Constructor
     *
     * @param pCause
     *            the exception
     * @since 1.0-SNAPSHOT
     */
    public PluginUtilsException(Throwable pCause) {
        super(pCause);
    }
}
