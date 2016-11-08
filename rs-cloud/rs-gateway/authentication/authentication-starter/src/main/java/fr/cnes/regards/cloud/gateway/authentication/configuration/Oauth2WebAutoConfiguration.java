/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import fr.cnes.regards.framework.authentication.Oauth2DefaultTokenMessageConverter;

/**
 *
 * Class MicroserviceWebConfiguration
 *
 * Configuration class for Spring Web Mvc. Http Message Converter specific for Oauth2Token. Oauth2Token are working with
 * Jackson. As we use Gson in the microservice-core we have to define here a specific converter.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class Oauth2WebAutoConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> pConverters) {
        pConverters.add(new Oauth2DefaultTokenMessageConverter());
        super.configureMessageConverters(pConverters);
    }
}
