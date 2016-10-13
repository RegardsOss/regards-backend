/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.report.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows to trace software requirements.
 *
 * @author msordi
 *
 */

@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Requirements.class)
@Documented
public @interface Requirement {

    /**
     *
     * @return the requirement reference
     */
    String value();
}
