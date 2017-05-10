/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

import com.google.gson.annotations.JsonAdapter;

import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Security hook to identify and secured REST endpoint accesses.
 *
 * @author msordi
 *
 */
@Documented
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("VOID")
@JsonAdapter(ResourceAccessAdapter.class)
public @interface ResourceAccess {

    /**
     * Describe the current feature should start with an action verb
     *
     * @return feature description
     * @since 1.0-SNAPSHOT
     */
    String description();

    /**
     *
     * If the resource access is a plugin implementation, this parameter allow to identify the plugin interface
     *
     * @return Plugin interface class
     * @since 1.0-SNAPSHOT
     */
    Class<?> plugin() default Void.class;

    /**
     * Allows to configure sensible default accesses
     *
     * @return default resource role
     */
    DefaultRole role() default DefaultRole.PROJECT_ADMIN;

}
