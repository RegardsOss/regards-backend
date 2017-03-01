/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.RequestInterceptor;

/**
 *
 * This class allows to customize Feign behavior.<br>
 * This class has to be annotated with <code>@Configuration</code>. <br/>
 * It uses an internal JWT with a system role to call another microservice.
 *
 * @author Marc Sordi
 *
 */
@Configuration
public class SysSecurityConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SysSecurityConfiguration.class);

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager securityManager;

    /**
     *
     * Interceptor for Feign client requests. This intercepter propagates the JWT token into requests header.
     *
     * @return RequestInterceptor
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return pRequestTemplate -> {
            // Insert system token into request header
            LOGGER.debug("Running Feign client request to : " + pRequestTemplate.request().url());
        };
    }

    /**
     * @return a system JWT with a system role
     */
    public String getJwt() {
        return securityManager.getSystemToken();
    }
}
