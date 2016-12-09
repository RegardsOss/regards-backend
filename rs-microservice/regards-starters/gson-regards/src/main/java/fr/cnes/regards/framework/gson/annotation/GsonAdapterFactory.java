/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;

/**
 * Identify a GSON factory to add dynamically to the {@link GsonBuilder} on startup
 *
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GsonAdapterFactory {

    Class<? extends TypeAdapterFactory> value();
}
