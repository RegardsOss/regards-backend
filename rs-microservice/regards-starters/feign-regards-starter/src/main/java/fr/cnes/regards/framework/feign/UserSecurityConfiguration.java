/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.RequestInterceptor;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * This class allows to customize Feign behavior.<br>
 * This class has to be annotated with <code>@Configuration</code>. <br/>
 * It uses and propagates user JWT to call another microservice.
 *
 * @author Marc Sordi
 *
 */
@Configuration
public class UserSecurityConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSecurityConfiguration.class);

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
            // Read token from SecurityContext. This is possible thanks to the spring hystrix configuration
            // hystrix.command.default..execution.isolation.strategy=SEMAPHORE
            final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                    .getAuthentication();
            // Insert token into request header
            if (authentication != null) {
                pRequestTemplate.header("Authorization", "Bearer " + authentication.getJwt());
            } else {
                LOGGER.error("No authentication found from security context.");
            }
            LOGGER.debug("Running Feign client request to : " + pRequestTemplate.request().url());
        };
    }
}
