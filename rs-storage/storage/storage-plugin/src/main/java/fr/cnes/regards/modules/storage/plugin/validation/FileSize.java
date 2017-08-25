/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/**
 *
 * Validation annotation, validated by {@link FileSizeValidator}
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(validatedBy = FileSizeValidator.class)
@Documented
public @interface FileSize {

    /**
     * Class to validate
     */
    static final String CLASS_NAME = "fr.cnes.regards.modules.storage.plugins.datastorage.domain.validation.FileSize.";

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
