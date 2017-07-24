/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.PathVariableParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestHeaderParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestParamParameterProcessor;
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
        return new SpringMvcContract(getCustomAnnotatedArgumentsProcessors());
    }

    /**
     * Customize the default AnnotatedArgumentsProcessors in order to use
     * our {@link CustomRequestParamParameterProcessor} instead of the {@link RequestParamParameterProcessor}
     * @return the list of processors
     */
    private List<AnnotatedParameterProcessor> getCustomAnnotatedArgumentsProcessors() {

        List<AnnotatedParameterProcessor> annotatedArgumentResolvers = new ArrayList<>();

        annotatedArgumentResolvers.add(new PathVariableParameterProcessor());
        annotatedArgumentResolvers.add(new CustomRequestParamParameterProcessor());
        annotatedArgumentResolvers.add(new RequestHeaderParameterProcessor());

        return annotatedArgumentResolvers;
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
