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
package fr.cnes.regards.framework.swagger.autoconfigure;

import fr.cnes.regards.framework.security.configurer.CustomWebSecurityConfigurationException;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.util.matcher.RequestMatcher;

import javax.servlet.http.HttpServletRequest;

/**
 * Class SwaggerWebSecurityConfiguration
 * <p>
 * Custom configuration to allow access to Swagger-UI endpoints
 *
 * @author Sébastien Binda
 */
public class SwaggerWebSecurityConfiguration implements ICustomWebSecurityConfiguration {

    @Override
    public void configure(final HttpSecurity http) throws CustomWebSecurityConfigurationException {
        http.requestMatcher(new RequestMatcher() {

            @Override
            public boolean matches(HttpServletRequest request) {
                return !getRequestPath(request).startsWith("/swagger-ui");
            }

            /**
             * Get path of the given HttpRequest
             *
             * @param request HttpRequest
             * @return String path
             */
            private String getRequestPath(final HttpServletRequest request) {
                String url = request.getServletPath();

                if (request.getPathInfo() != null) {
                    url += request.getPathInfo();
                }

                return url;
            }
        });
    }

}
