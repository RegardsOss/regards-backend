/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.authentication.internal;

import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * Class NotOAuthRequestMatcher
 * <p>
 * Filter for authentication oauth2 endpoints
 *
 * @author Sébastien Binda
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
     * Get path of the given HttpRequest
     *
     * @param pRequest HttpRequest
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
