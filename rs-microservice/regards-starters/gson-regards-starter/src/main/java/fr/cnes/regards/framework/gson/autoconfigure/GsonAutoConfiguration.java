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
package fr.cnes.regards.framework.gson.autoconfigure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.GsonProperties;

/**
 * GSON support auto configuration
 *
 * @author Marc Sordi
 */
@Configuration
@EnableConfigurationProperties(GsonProperties.class)
@AutoConfigureBefore({ HttpMessageConvertersAutoConfiguration.class })
public class GsonAutoConfiguration implements ApplicationContextAware {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GsonAutoConfiguration.class);

    /**
     * Factory to adapter swagger documentation serialization
     */
    private static final String SPRINGFOX_GSON_FACTORY = "fr.cnes.regards.framework.swagger.gson.SpringFoxTypeFactory";

    @Autowired
    private GsonProperties properties;

    /**
     * Spring application context
     */
    private ApplicationContext applicationContext;

    /**
     * Configure a builder with GSON adapter for Sprinfox swagger Json object
     *
     * @return {@link GsonBuilder}
     */
    @Bean
    @ConditionalOnClass(name = SPRINGFOX_GSON_FACTORY)
    public GsonBuilder configureWithSwagger(GsonBuilderFactory gsonBuilderFactory) {
        LOGGER.info("GSON auto configuration enabled with SpringFox support");
        GsonBuilder builder = gsonBuilderFactory.newBuilder();
        try {
            builder.registerTypeAdapterFactory((TypeAdapterFactory) Class.forName(SPRINGFOX_GSON_FACTORY)
                    .newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            final String errorMessage = "Cannot init SpringFox GSON factory";
            LOGGER.error(errorMessage, e);
            throw new UnsupportedOperationException(errorMessage);
        }
        return builder;
    }

    @Bean
    @ConditionalOnMissingClass(SPRINGFOX_GSON_FACTORY)
    public GsonBuilder configure(GsonBuilderFactory gsonBuilderFactory) {
        LOGGER.info("GSON auto configuration enabled");
        return gsonBuilderFactory.newBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public Gson gson(GsonBuilder builder) {
        return builder.create();
    }

    @Bean
    @ConditionalOnMissingBean
    public GsonBuilderFactory gsonBuilderFactory() {
        return new GsonBuilderFactory(properties, applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public GsonHttpMessageConverter gsonConverter(Gson gson) {
        final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(gson);
        return gsonHttpMessageConverter;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
