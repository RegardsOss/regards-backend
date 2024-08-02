/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.feign;

import com.google.gson.Gson;
import feign.*;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.feign.FeignDecorators;
import io.github.resilience4j.feign.Resilience4jFeign;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.support.PageableSpringEncoder;
import org.springframework.cloud.openfeign.support.PageableSpringQueryMapEncoder;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Common client configuration between sys and user.<br/>
 * This class allows to customize Feign behavior.<br>
 * This class has to be annotated with <code>@Configuration</code>.
 *
 * @author Sébastien Binda
 */
@Configuration
public class FeignClientConfiguration {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(FeignClientConfiguration.class);

    /**
     * Basic log
     * @return loggin level
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }

    /**
     * Specific error analyzer for feign client error responses.
     */
    @Bean
    public ClientErrorDecoder errorDecoder() {
        return new ClientErrorDecoder();
    }

    /**
     * Every REGARDS clients should use a Gson decoder/encoder.
     */
    @Bean
    public Decoder getDecoder(@Autowired(required = false) Gson gson) {
        if (gson != null) {
            return new ResponseEntityDecoder(new GsonDecoder(gson));
        }
        return new ResponseEntityDecoder(new GsonDecoder());
    }

    /**
     * Every REGARDS clients should use a Gson decoder/encoder.
     */
    @Bean
    public Encoder getEncoder(@Autowired(required = false) Gson gson) {
        if (gson != null) {
            return new PageableSpringEncoder(new GsonEncoder(gson));
        }
        return new PageableSpringEncoder(new GsonEncoder());
    }

    /**
     * To use @SpringQueryMap annotation on a method parameter (further decoded as both MultiValueMap and Pageable
     * arguments into REST method)
     */
    @Bean
    public QueryMapEncoder queryMapEncoder() {
        return new PageableSpringQueryMapEncoder();
    }

    /**
     * Enable Spring MVC contract concept
     */
    @Bean
    public Contract feignContract() {
        return new FeignContractSupplier().get();
    }

    /**
     * Allow 404 response to be processed as empty response insetad of error.
     */
    @Bean
    public Feign.Builder builder(@Value("${regards.enable.feign.bulkhead:true}") boolean bulkhead,
                                 @Value("${regards.enable.feign.rateLimiter:true}") boolean rateLimiter,
                                 @Value("${regards.enable.feign.readTimeout:60000}") int readTimeout,
                                 @Value("${regards.enable.feign.bulkhead.maxWaitDurationInSeconds:3600}")
                                 long maxWaitDurationInSeconds) {

        LOGGER.info("Initialization of feign configuration with bulkhead={}, rateLimiter={}", bulkhead, rateLimiter);
        FeignDecorators.Builder feignDecoratorBuilder = FeignDecorators.builder();
        if (rateLimiter) {
            // configure feign with custom rate limiter
            RateLimiter rateLimitConfig = RateLimiter.of("customRateLimiter",
                                                         RateLimiterConfig.custom()
                                                                          .limitRefreshPeriod(Duration.ofMillis(1L))
                                                                          .limitForPeriod(50)
                                                                          .timeoutDuration(Duration.ofMillis(readTimeout))
                                                                          .build());
            feignDecoratorBuilder.withRateLimiter(rateLimitConfig);
        }

        if (bulkhead) {
            // configure feign with semaphore-based bulkhead
            feignDecoratorBuilder.withBulkhead(Bulkhead.of("customBulkhead",
                                                           BulkheadConfig.custom()
                                                                         .maxWaitDuration(Duration.ofSeconds(
                                                                             maxWaitDurationInSeconds))
                                                                         .build()));
        }

        // return custom feign builder and allow 404 responses to be processed without errors
        Resilience4jFeign.Builder builder = Resilience4jFeign.builder(feignDecoratorBuilder.build());
        builder.dismiss404();
        builder.options(new Request.Options(5000, TimeUnit.MILLISECONDS, readTimeout, TimeUnit.MILLISECONDS, false));
        return builder;
    }
}
