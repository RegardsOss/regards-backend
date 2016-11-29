/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author Xavier-Alexandre Brochard
 */
public final class ValidationUrlBuilder {

    private ValidationUrlBuilder() {

    }

    /**
     * Build the validation url from the current request context
     *
     * @param pRequest
     *            the current request context
     * @return the validation url
     */
    public static String buildFrom(final HttpServletRequest pRequest) {
        return "http://" + pRequest.getServerName() + ":" + pRequest.getServerPort() + pRequest.getContextPath();
    }

}
