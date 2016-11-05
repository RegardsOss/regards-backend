/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins;

/**
 *
 * Authentication error exception.
 *
 * @author Sébastien Binda
 *
 */
public class UserNotFoundException extends Exception {

    /**
     * Serial version
     */
    private static final long serialVersionUID = 7134517054707247751L;

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Throwable cause) {
        super(cause);
    }

}
