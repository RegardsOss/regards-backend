/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;

import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

/**
 *
 * Common client configuration between sys and user.<br/>
 * This class allows to customize Feign behavior.<br>
 * This class has to be annotated with <code>@Configuration</code>.
 *
 * @see http://projects.spring.io/spring-cloud/spring-cloud.html#spring-cloud-feign
 *
 *
 * @author Sébastien Binda
 * @since 1.0-SNPASHOT
 */
@Configuration
public class FeignClientConfiguration {

    /**
     * Basic log
     *
     * @return loggin level
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
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
    public Decoder getDecoder(@Autowired(required = false) Gson pGson) {
        if (pGson != null) {
            return new ResponseEntityDecoder(new GsonDecoder(pGson));
        }
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
    public Encoder getEncoder(@Autowired(required = false) Gson pGson) {
        if (pGson != null) {
            return new GsonEncoder(pGson);
        }
        return new GsonEncoder();
    }

    /**
     * Enable Spring MVC contract concept
     *
     * @return {@link Contract}
     */
    @Bean
    public Contract feignContract() {
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
        Feign.Builder builder = Feign.builder();
        builder.decode404();
        return builder;
    }
}
