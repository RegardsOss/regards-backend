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

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface GSonIgnore {
    // Field tag only annotation
}
