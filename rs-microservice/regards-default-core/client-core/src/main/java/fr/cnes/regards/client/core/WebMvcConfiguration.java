/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.client.core;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration.EnableWebMvcConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcProperties;
import org.springframework.boot.autoconfigure.web.WebMvcRegistrations;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 *
 * Class WebMvcConfiguration
 *
 * Update Spring Web Mvc configuration to ignore RequestMapping on FeignClient implementations.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Configuration
@ConditionalOnWebApplication
public class WebMvcConfiguration extends EnableWebMvcConfiguration {

    public WebMvcConfiguration(final ObjectProvider<WebMvcProperties> pMvcPropertiesProvider,
            final ObjectProvider<WebMvcRegistrations> pMvcRegistrationsProvider,
            final ListableBeanFactory pBeanFactory) {
        super(pMvcPropertiesProvider, pMvcRegistrationsProvider, pBeanFactory);
    }

    @Override
    protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
        return new RequestMappingHandlerMapping() {

            @Override
            protected boolean isHandler(final Class<?> beanType) {
                return super.isHandler(beanType)
                        && (AnnotationUtils.findAnnotation(beanType, FeignClient.class) == null);
            }
        };
    }
}
