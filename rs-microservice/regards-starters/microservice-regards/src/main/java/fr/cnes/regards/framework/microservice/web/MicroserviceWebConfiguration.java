/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.web;

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
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
public class MicroserviceWebConfiguration extends WebMvcConfigurerAdapter {

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
