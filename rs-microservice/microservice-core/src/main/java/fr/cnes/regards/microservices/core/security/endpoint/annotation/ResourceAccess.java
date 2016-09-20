/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.security.endpoint.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Security hook to identify and secured REST endpoint accesses.
 * 
 * @author msordi
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("VOID")
public @interface ResourceAccess {

    /**
     * Set the name of the current resource.<br>
     * Resource name is associated to the HTTP verb to identify the resource access (i.e. the endpoint). This
     * association must be unique by microservice. Default value ""
     *
     *
     * @return resource name.
     */
    String name() default "";

    /**
     * Describe the current feature should start with an action verb
     *
     * @return feature description
     */
    String description();
}
