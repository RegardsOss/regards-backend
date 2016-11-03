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
 * Annotate a plugin parameter. Following field types are supported for injection :
 * <ul>
 * <li>String</li>
 * <li>Byte</li>
 * <li>Short</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Float</li>
 * <li>Double</li>
 * <li>Boolean</li>
 * </ul>
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginParameter {

    /**
     *
     * Plugin parameter name. The parameter name defined here are managed by the PluginManager service.
     *
     * @return the plugin parameter name
     */
    String name();

    /**
     *
     * Parameter description to explain the expected value if the name is not explicit enough.
     *
     * @return plugin parameter's description
     */
    String description() default "";

}
