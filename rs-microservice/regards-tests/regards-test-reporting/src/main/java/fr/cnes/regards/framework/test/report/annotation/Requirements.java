/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.report.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * List of method requirements
 *
 * @author msordi
 *
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Requirements {

    /**
     *
     * @return the list of requirements
     */
    Requirement[] value();

}
