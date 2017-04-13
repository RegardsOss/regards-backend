/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.plugins.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception for plugin utils package. It usually means that the plugin couldn't be instanciated.
 *
 * @author Christophe Mertz
 */
public class PluginUtilsRuntimeException extends RuntimeException {

    /**
     * Class Logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginUtilsRuntimeException.class);

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 476540009609921511L;

    /**
     * Constructor
     *
     * @param pMessage an error message
     */
    public PluginUtilsRuntimeException(final String pMessage) {
        super(pMessage);
        LOGGER.error(pMessage);
    }

    /**
     * Constructor
     *
     * @param pMessage an error message
     * @param pCause the exception
     */
    public PluginUtilsRuntimeException(final String pMessage, final Throwable pCause) {
        super(pMessage, pCause);
        LOGGER.error(pMessage, pCause);
    }

    /**
     * Constructor
     *
     * @param pCause the exception
     */
    public PluginUtilsRuntimeException(final Throwable pCause) {
        super(pCause);
    }
}
