/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils;

/**
 * Security constants
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 *
 */
public final class HttpConstants {

    /**
     * Authorization header
     */
    public static final String AUTHORIZATION = "Authorization";

    /**
     * Content-type
     */
    public static final String CONTENT_TYPE = "Content-type";

    /**
     * Accept type
     */
    public static final String ACCEPT = "Accept";

    /**
     * Authorization header scheme
     */
    public static final String BEARER = "Bearer";

    /**
     * Scope parameter to read in Header or request query parameters
     */
    public static final String SCOPE = "scope";

    private HttpConstants() {
    }
}
