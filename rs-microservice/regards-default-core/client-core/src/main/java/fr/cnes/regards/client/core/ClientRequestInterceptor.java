/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.RequestInterceptor;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Class ClientRequestInterceptor
 *
 * Interceptor for Feign client requsts. This interceptor add the JWT token into requests header.
 *
 * @author CS
 * @since 1.0-SNPASHOT
 */
@Configuration
public class ClientRequestInterceptor {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ClientRequestInterceptor.class);

    /**
     *
     * Create request interceptor in spring context
     *
     * @return RequestInterceptor
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return pRequestTemplate -> {
            // Read token from SecurityContext. This is possible thanks to the spring hystrix configuration
            // hystrix.command.default..execution.isolation.strategy=SEMAPHORE
            final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            // Insert token into request header
            pRequestTemplate.header("Authorization", "Bearer " + authentication.getJwt());

            LOG.info("Running Feign client request to : " + pRequestTemplate.request().url());
        };
    }

    @Bean
    public ClientErrorDecoder errorDecoder() {
        return new ClientErrorDecoder();
    }

}
