/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.core.annotation.AliasFor;

import feign.Headers;
import fr.cnes.regards.framework.feign.FeignClientConfiguration;
import fr.cnes.regards.framework.feign.UserSecurityConfiguration;

/**
 *
 * Class RestClient
 *
 * Annotation for all microservice clients
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@FeignClient(configuration = { FeignClientConfiguration.class, UserSecurityConfiguration.class })
@Headers({ "Accept: application/json", "Content-Type: application/json" })
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface UserFeignClient {

    /**
     *
     * Name of the microservice as it is registered in the eureka server.
     *
     * @return name
     * @since 1.0-SNAPSHOT
     */
    @AliasFor(annotation = FeignClient.class)
    String name();

    /**
     *
     * Fallback class implementation for the client
     *
     * @return Class
     * @since 1.0-SNAPSHOT
     */
    @AliasFor(annotation = FeignClient.class)
    Class<?> fallback() default void.class;

}
