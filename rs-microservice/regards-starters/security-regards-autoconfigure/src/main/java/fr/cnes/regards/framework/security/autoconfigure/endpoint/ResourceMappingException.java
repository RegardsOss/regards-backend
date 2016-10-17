/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.endpoint;

/**
 * This exception is thrown when resource have insufficient or inconsistent security configuration
 *
 * @author msordi
 *
 */
public class ResourceMappingException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Resource mapping exception
     *
     * @param pMessage
     *            error message
     */
    public ResourceMappingException(String pMessage) {
        super(pMessage);
    }
}
