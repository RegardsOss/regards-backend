/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(validatedBy = DirectoryValidator.class)
@Documented
public @interface Directory {

    /**
     * Class to validate
     */
    static final String CLASS_NAME = "fr.cnes.regards.modules.storage.plugins.datastorage.domain.validation.Directory.";

    /**
     *
     * @return error message key
     */
    String message() default "{" + CLASS_NAME + "message}";

    /**
     *
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     *
     * @return custom payload
     */
    Class<? extends Payload>[] payload() default {};

}
