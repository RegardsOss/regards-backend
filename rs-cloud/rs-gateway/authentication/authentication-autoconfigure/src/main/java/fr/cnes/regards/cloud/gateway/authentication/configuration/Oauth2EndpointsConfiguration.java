/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.provider.endpoint.FrameworkEndpointHandlerMapping;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.configurer.ICustomWebSecurityConfiguration;
import fr.cnes.regards.framework.security.filter.IpFilter;

/**
 *
 * Class Oauth2EndpointsConfiguration
 *
 * Custom configuration to allow access to Oauth2 tokens
 *
 * @author Sébastien Binda
 *
 */
@Component
public class Oauth2EndpointsConfiguration implements ICustomWebSecurityConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2EndpointsConfiguration.class);

    /**
     * Oauth2 endpoints to allow
     */
    @Autowired(required = false)
    private AuthorizationServerEndpointsConfiguration endpoints;

    /**
     *
     * Match to allow access to Oauth2 endpoints
     *
     * @author Sébastien Binda
     *
     */
    private static class NotOAuthRequestMatcher implements RequestMatcher {

        /**
         * Oauth2 endpoints mapping
         */
        private final FrameworkEndpointHandlerMapping mapping;

        public NotOAuthRequestMatcher(FrameworkEndpointHandlerMapping mapping) {
            this.mapping = mapping;
        }

        @Override
        public boolean matches(HttpServletRequest request) {
            final String requestPath = getRequestPath(request);
            for (final String path : mapping.getPaths()) {
                if (requestPath.startsWith(mapping.getPath(path))) {
                    return false;
                }
            }
            return true;
        }

        /**
         *
         * Get path of the given HttpRequest
         *
         * @param request
         *            HttpRequest
         * @return String path
         */
        private String getRequestPath(HttpServletRequest request) {
            String url = request.getServletPath();

            if (request.getPathInfo() != null) {
                url += request.getPathInfo();
            }

            return url;
        }

    }

    @Override
    public void configure(final HttpSecurity pHttp) throws Exception {
        if (endpoints != null) {
            LOG.info("[REGARDS AUTHENTICATION MODULE] Adding  specific web security to allow oauth2 endpoint access");
            // Assume we are in an Authorization Server
            pHttp.requestMatcher(new NotOAuthRequestMatcher(endpoints.oauth2EndpointHandlerMapping()));
        }

        // Deny access to all SYS roles
        pHttp.addFilterAfter(new RoleSysFilter(), IpFilter.class);
    }

}
