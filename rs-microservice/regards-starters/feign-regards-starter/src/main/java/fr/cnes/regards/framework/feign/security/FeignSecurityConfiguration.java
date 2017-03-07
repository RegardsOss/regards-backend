/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.security;

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
public class FeignSecurityConfiguration {

    /**
     *
     * Interceptor for Feign client request security. This interceptor injects a token into request headers.
     *
     * @param pFeignSecurityManager
     *            the Feign security manager
     * @return RequestInterceptor custom system interceptor
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public RequestInterceptor securityRequestInterceptor(FeignSecurityManager pFeignSecurityManager) {
        return new FeignSecurityInterceptor(pFeignSecurityManager);
    }
}
