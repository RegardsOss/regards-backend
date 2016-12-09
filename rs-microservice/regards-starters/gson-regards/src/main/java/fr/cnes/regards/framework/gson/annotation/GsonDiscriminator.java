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
 * Annotation to set sub type discriminator value. This annotation is optional and allows to override default value.
 *
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GsonDiscriminator {

    /**
     * @return discriminator value to set on {@link PolymorphicTypeAdapterFactory}
     */
    String value();
}
