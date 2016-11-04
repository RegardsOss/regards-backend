/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.plugins.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * Annotate a plugin type.
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginInterface {

    /**
     *
     * Parameter description to explain the expected value if the name is not explicit enough.
     *
     * @return plugin type's description
     */
    String description() default "";

}
