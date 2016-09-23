/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MicroserviceInfo {

    /**
     *
     * @return name of the microservice
     */
    String name();

    /**
     *
     * @return version of the microservice
     */
    String version();

    /**
     *
     * @return dependencies of microservice
     */
    String[] dependencies() default {};
}
