/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint;

/**
 * This exception is thrown when resource have insufficient or inconsistent security configuration
 *
 * @author msordi
 *
 */
public class ResourceMappingException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResourceMappingException(String pMessage) {
        super(pMessage);
    }
}
