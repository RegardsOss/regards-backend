/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.web;

import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * Class MicroserviceWebConfiguration
 *
 * Configuration class for Spring Web Mvc.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceWebConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> pConverters) {
        pConverters.add(new GsonHttpMessageConverter());
        super.configureMessageConverters(pConverters);
    }

    @Override
    public void configurePathMatch(final PathMatchConfigurer pMatcher) {
        pMatcher.setUseRegisteredSuffixPatternMatch(true);
    }

    @Override
    public void configureContentNegotiation(final ContentNegotiationConfigurer configurer) {
        // Tell Spring to not interprete the path extensions as request media type
        // We must disable this in order to user emails in path variables (like "/user/user@email.com") otherwise
        // Spring will try to interprete ".com" as a media type (just like ".pdf", ".html"...)
        configurer.favorPathExtension(false);
    }

}
