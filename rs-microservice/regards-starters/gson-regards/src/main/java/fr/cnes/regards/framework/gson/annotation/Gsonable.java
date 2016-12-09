/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;

/**
 *
 * Annotation allowing us to automatically add a class and it's subtypes to a {@link PolymorphicTypeAdapterFactory}
 *
 * @author Sylvain Vissiere-Guerinet
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Gsonable {

    /**
     * Specify the discriminator field name for serialization and deserialization. If it is empty(default) then a
     * discriminant field will be added to serialized object.
     *
     * @return discriminant name
     */
    String value() default "";
}
