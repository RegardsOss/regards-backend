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

import fr.cnes.regards.framework.security.configurer.CustomWebSecurityConfigurationException;
import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityAuthorizeRequestsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;

/**
 * Class Oauth2EndpointsConfiguration
 * <p>
 * Custom configuration to allow access to Oauth2 tokens
 *
 * @author Sébastien Binda
 */
public class Oauth2EndpointsConfiguration implements ICustomWebSecurityAuthorizeRequestsConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2EndpointsConfiguration.class);

    /**
     * Oauth2 endpoints to allow
     */
    private final AuthorizationServerEndpointsConfiguration endpoints;

    public Oauth2EndpointsConfiguration(final AuthorizationServerEndpointsConfiguration pEndpoints) {
        super();
        endpoints = pEndpoints;
    }

    @Override
    public void configure(final HttpSecurity http) throws CustomWebSecurityConfigurationException {
        if (endpoints != null) {
            LOG.info("[REGARDS AUTHENTICATION MODULE] Adding specific web security to allow oauth2 endpoint access");
            try {
                // Assume we are in an Authorization Server
                // We cannot use authorizeRequests().requestMatchers(...) here
                // see https://github.com/spring-attic/spring-security-oauth/issues/634
                http.requestMatcher(new NotOAuthRequestMatcher(endpoints.oauth2EndpointHandlerMapping()));
            } catch (final Exception e) {
                LOG.warn("An error occurred during the configuration of custom web security", e);
                throw new CustomWebSecurityConfigurationException(e.getCause());
            }
        }
    }

}
