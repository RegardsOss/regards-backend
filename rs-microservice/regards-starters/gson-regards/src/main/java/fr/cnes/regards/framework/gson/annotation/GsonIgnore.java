/*
 * LICENSE_PLACEHOLDER
 */

/**
 *
 * Class GSonIgnore
 *
 * @author Christophe Mertz
 */
package fr.cnes.regards.framework.gson.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a field to be ignore by GSON
 * 
 * @author Marc Sordi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface GsonIgnore {
    // Field tag only annotation
}
