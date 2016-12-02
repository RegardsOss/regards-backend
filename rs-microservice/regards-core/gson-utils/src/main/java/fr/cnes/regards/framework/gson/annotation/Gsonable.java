/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * annotation allowing us to automatically add a class and it's subtypes to a TypeAdapterFactory
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Gsonable {

    /**
     * Specify the discriminator field name for serialization and deserialization. If it is empty(default) then a
     * discriminent field will be added to serialized object.
     *
     */
    String value() default "";

}
