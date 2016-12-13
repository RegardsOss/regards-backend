/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.stereotype.Component;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

/**
 * Identify a GSON {@link TypeAdapterFactory} to be added dynamically to the {@link GsonBuilder} at startup. The target
 * factory is managed by Spring so we can use dependency injection.
 *
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface GsonTypeAdapterFactoryBean {
}
