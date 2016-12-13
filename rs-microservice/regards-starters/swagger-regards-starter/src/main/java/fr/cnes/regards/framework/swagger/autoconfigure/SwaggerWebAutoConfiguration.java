/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.swagger.autoconfigure;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 *
 * Class SwaggerWebAutoConfiguration
 *
 * Spring Autoconfiguration class to configue speicfic HttpMessage converter for springfox objects.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class SwaggerWebAutoConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> pConverters) {
        pConverters.add(new SwaggerMessageConverter());
        super.configureMessageConverters(pConverters);
    }

}
