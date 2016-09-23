/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.modules.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {

    /**
     *
     * @return name of the module
     */
    String name();

    /**
     *
     * @return description of the module
     */
    String description() default "";

    /**
     *
     * @return version of the module
     */
    String version();

    /**
     *
     * @return author of the module
     */
    String author();

    /**
     *
     * @return legal owner of the module
     */
    String legalOwner();

    /**
     *
     * @return link to the documentation of the module
     */
    String documentation();

}
