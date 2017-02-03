/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.client.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import feign.Contract;
import feign.Feign;
import feign.RequestInterceptor;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;

/**
 *
 * Class FeignClientConfiguration
 *
 * Configuration class for all Feign clients
 *
 * @author Sébastien Binda
 * @since 1.0-SNPASHOT
 */
@Configuration
public class FeignClientConfiguration {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(FeignClientConfiguration.class);

    /**
     *
     * Intercepter for Feign client requests. This intercepter add the JWT token into requests header.
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
            }

            LOG.info("Running Feign client request to : " + pRequestTemplate.request().url());
        };
    }

    /**
     *
     * Specific error analyzer for feign client error responses.
     *
     * @return ClientErrorDecoder
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public ClientErrorDecoder errorDecoder() {
        return new ClientErrorDecoder();
    }

    /**
     *
     * Every REGARDS clients should use a Gson decoder/encoder.
     *
     * @see {@link MicroserviceWebConfiguration}
     *
     * @return Decoder
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public Decoder getDecoder() {
        return new ResponseEntityDecoder(new GsonDecoder());
    }

    /**
     *
     * Every REGARDS clients should use a Gson decoder/encoder.
     *
     * @see {@link MicroserviceWebConfiguration}
     *
     * @return Encoder
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public Encoder getEncoder() {
        return new GsonEncoder();
    }

    @Bean
    public Contract feignContractg() {
        return new SpringMvcContract();
    }

    /**
     *
     * Allow 404 response to be process not like errors.
     *
     * @return Feign Builder
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public Feign.Builder builder() {
        final Feign.Builder builder = new HystrixFeign.Builder();
        builder.decode404();
        return builder;
    }

}
