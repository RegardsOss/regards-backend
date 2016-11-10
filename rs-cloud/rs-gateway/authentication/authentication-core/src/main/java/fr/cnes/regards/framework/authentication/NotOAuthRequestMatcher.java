/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 *
 * Class NotOAuthRequestMatcher
 *
 * Filter for authentication oauth2 endpoints
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class NotOAuthRequestMatcher implements RequestMatcher {

    /**
     * Oauth2 endpoints mapping
     */
    private final FrameworkEndpointHandlerMapping mapping;

    public NotOAuthRequestMatcher(final FrameworkEndpointHandlerMapping pMapping) {
        this.mapping = pMapping;
    }

    @Override
    public boolean matches(final HttpServletRequest pRequest) {
        boolean result = true;
        final String requestPath = getRequestPath(pRequest);
        for (final String path : mapping.getPaths()) {
            if (requestPath.startsWith(mapping.getPath(path))) {
                result = false;
            }
        }
        return result;
    }

    /**
     *
     * Get path of the given HttpRequest
     *
     * @param pRequest
     *            HttpRequest
     * @return String path
     */
    private String getRequestPath(final HttpServletRequest pRequest) {
        String url = pRequest.getServletPath();

        if (pRequest.getPathInfo() != null) {
            url += pRequest.getPathInfo();
        }

        return url;
    }

}
