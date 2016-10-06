package fr.cnes.regards.client.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import fr.cnes.regards.security.utils.jwt.JWTAuthentication;

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
     *
     * Create request interceptor in spring context
     *
     * @return RequestInterceptor
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public RequestInterceptor requestTokenBearerInterceptor() {
        return new RequestInterceptor() {

            @Override
            public void apply(RequestTemplate pRequestTemplate) {
                // Read token from SecurityContext. This is possible thanks to the spring hystrix configuration
                // hystrix.command.default..execution.isolation.strategy=SEMAPHORE
                final JWTAuthentication authentication = (JWTAuthentication) SecurityContextHolder.getContext()
                        .getAuthentication();
                // Insert token into request header
                pRequestTemplate.header("Authorization", "Bearer " + authentication.getJwt());
            }
        };
    }

}
