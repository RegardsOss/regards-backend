/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.configurer;

/**
 * Exception which indicates an error occurred during the configuration of custom web security.
 *
 * @author Xavier-Alexandre Brochard
 */
public class CustomWebSecurityConfigurationException extends Exception {

    /**
     * Serial
     */
    private static final long serialVersionUID = 7027393628503256052L;

    /**
     * Constructs a new exception with the specified cause.
     */
    public CustomWebSecurityConfigurationException(final Throwable pCause) {
        super("An error occurred during the configuration of custom web security ", pCause);
    }

}
