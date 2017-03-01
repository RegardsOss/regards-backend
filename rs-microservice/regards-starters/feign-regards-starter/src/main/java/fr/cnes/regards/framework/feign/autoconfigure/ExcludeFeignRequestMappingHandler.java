/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.autoconfigure;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 *
 * Class CustomRequestMappingHandler
 *
 * Update Spring Web Mvc configuration to ignore RequestMapping on FeignClient implementations.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ExcludeFeignRequestMappingHandler extends RequestMappingHandlerMapping { // NOSONAR

    @Override
    protected boolean isHandler(final Class<?> beanType) {
        return super.isHandler(beanType) && (AnnotationUtils.findAnnotation(beanType, FeignClient.class) == null);
    }

}
