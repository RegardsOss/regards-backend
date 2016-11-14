/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.authentication.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;

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
public class Oauth2EndpointsConfiguration implements ICustomWebSecurityConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(Oauth2EndpointsConfiguration.class);

    /**
     * Oauth2 endpoints to allow
     */
    // @Autowired(required = false)
    private final AuthorizationServerEndpointsConfiguration endpoints;

    public Oauth2EndpointsConfiguration(final AuthorizationServerEndpointsConfiguration pEndpoints) {
        super();
        endpoints = pEndpoints;
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
