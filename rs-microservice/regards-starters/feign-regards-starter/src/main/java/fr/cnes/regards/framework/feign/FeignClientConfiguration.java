/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Request;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Common client configuration between sys and user.<br/>
 * This class allows to customize Feign behavior.<br>
 * This class has to be annotated with <code>@Configuration</code>.
 * @author Sébastien Binda
 */
@Configuration
public class FeignClientConfiguration {

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
     * @return ClientErrorDecoder
     */
    @Bean
    public ClientErrorDecoder errorDecoder() {
        return new ClientErrorDecoder();
    }

    /**
     * Every REGARDS clients should use a Gson decoder/encoder.
     * @return Decoder
     */
    @Bean
    public Decoder getDecoder(@Autowired(required = false) Gson pGson) {
        if (pGson != null) {
            return new ResponseEntityDecoder(new GsonDecoder(pGson));
        }
        return new ResponseEntityDecoder(new GsonDecoder());
    }

    /**
     * Every REGARDS clients should use a Gson decoder/encoder.
     * @return Encoder
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
     * @return {@link Contract}
     */
    @Bean
    public Contract feignContract() {
        return new FeignContractSupplier().get();
    }

    /**
     * Allow 404 response to be process not like errors.
     * @return Feign Builder
     */
    @Bean
    public Feign.Builder builder() {
        // configure feign with custom rate limiter
        RateLimiter rateLimitConfig = RateLimiter.of("customRateLimiter",
                                             RateLimiterConfig.custom()
                                                           .limitRefreshPeriod(Duration.ofMillis(1L))
                                                           .limitForPeriod(50)
                                                           .timeoutDuration(Duration.ofSeconds(60L))
                                                           .build());
        // configure feign with semaphore-based bulkhead
        Bulkhead bulkhead = Bulkhead.of("customBulkhead",
                              BulkheadConfig.custom()
                                    .build());
        FeignDecorators feignDecorators = FeignDecorators.builder()
                                                         .withRateLimiter(rateLimitConfig)
                                                         .withBulkhead(bulkhead).build();

        // return custom feign builder and allow 404 responses to be processed without errors
        Resilience4jFeign.Builder builder =  Resilience4jFeign.builder(feignDecorators);
        builder.decode404();
        builder.options(new Request.Options(5000, TimeUnit.MILLISECONDS, 60000, TimeUnit.MILLISECONDS, false));
        return builder;
    }
}
