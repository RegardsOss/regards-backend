/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.web;

import java.nio.file.Path;
import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 *
 * Class MicroserviceWebConfiguration
 *
 * Configuration class for Spring Web Mvc.
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceWebConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> pConverters) {
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Path.class, new PathAdapter().nullSafe());
        final Gson gson = builder.create();
        final GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
        gsonHttpMessageConverter.setGson(gson);

        pConverters.add(gsonHttpMessageConverter);
        super.configureMessageConverters(pConverters);
    }

    @Override
    public void configurePathMatch(final PathMatchConfigurer pConfigurer) {
        pConfigurer.setUseSuffixPatternMatch(false);
        super.configurePathMatch(pConfigurer);
    }

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer pConfigurer) {
        // Avoid to match uri path extension with a content negociator.
        pConfigurer.favorPathExtension(false);
        super.configureContentNegotiation(pConfigurer);
    }

}
