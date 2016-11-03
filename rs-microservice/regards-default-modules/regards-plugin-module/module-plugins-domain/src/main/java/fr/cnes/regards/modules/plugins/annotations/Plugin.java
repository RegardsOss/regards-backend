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
 * Class RegardsPlugin
 *
 * Main representation of plugin meta-data.
 *
 * @author Christophe Mertz
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Plugin {

    /**
     *
     * Unique Id of the plugin. If the given id is not unique, an error is thrown during plugin loading. Class canonical
     * name is retain as a default value.
     *
     * @return the plugin's id
     */
    String id() default "";

    /**
     *
     * Version of the plugin. Use to check if the plugin changed
     *
     * @return the plugin's version
     */
    String version();

    /**
     *
     * Author of the plugin. Simple user information.
     *
     * @return the plugin's author
     */
    String author();

    /**
     *
     * Description of the plugin. Simple user information.
     *
     * @return the plugin's description
     */
    String description();

}
