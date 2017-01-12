/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.configuration;

import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import fr.cnes.regards.framework.authentication.internal.Oauth2DefaultTokenMessageConverter;
import fr.cnes.regards.framework.authentication.internal.controller.InternalAuthenticationController;
import fr.cnes.regards.framework.authentication.internal.service.IInternalAuthenticationPluginsService;
import fr.cnes.regards.framework.authentication.internal.service.InternalAuthenticationPluginService;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;

/**
 *
 * Class MicroserviceWebConfiguration
 *
 * Configuration class for Spring Web Mvc.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class Oauth2WebAutoConfiguration extends WebMvcConfigurerAdapter {

    /**
     *
     * Http Message Converter specific for Oauth2Token. Oauth2Token are working with Jackson. As we use Gson in the
     * microservice-core we have to define here a specific converter.
     *
     * @see org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter#configureMessageConverters(java.util.List)
     * @since 1.0-SNAPSHOT
     */
    @Override
    public void configureMessageConverters(final List<HttpMessageConverter<?>> pConverters) {
        pConverters.add(new Oauth2DefaultTokenMessageConverter());
        super.configureMessageConverters(pConverters);
    }

    /**
     *
     * Create RestController to manage authentication Identity Provider plugins.
     *
     * @param pPluginService
     *            Plugin service
     * @return {@link InternalAuthenticationController}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public InternalAuthenticationController internalAuthenticationController(final IPluginService pPluginService) {
        return new InternalAuthenticationController(internalAuthenticationPluginService(pPluginService));
    }

    /**
     *
     * Create Service to manage identity provider plugins
     *
     * @param pPluginService
     *            Plugin service
     * @return {@link IInternalAuthenticationPluginsService}
     * @since 1.0-SNAPSHOT
     */
    @Bean
    public IInternalAuthenticationPluginsService internalAuthenticationPluginService(
            final IPluginService pPluginService) {
        return new InternalAuthenticationPluginService(pPluginService);
    }

}
