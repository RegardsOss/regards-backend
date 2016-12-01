/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.registration;

import javax.servlet.http.HttpServletRequest;

/**
 * Holds builder methods returning the current request url. TODO: Move
 *
 * @author Xavier-Alexandre Brochard
 */
public final class AppUrlBuilder {

    private AppUrlBuilder() {

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
