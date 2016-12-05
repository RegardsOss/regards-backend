/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.validator;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, ANNOTATION_TYPE })
@Constraint(validatedBy = PastOrNowValidator.class)
@Documented
/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public @interface PastOrNow {

    /**
     * stored as a constant because could not use this.getClass().getName()
     */
    static final String CLASS_NAME = "fr.cnes.regards.framework.jpa.validator.PastOrNow.";

    String message() default "{" + CLASS_NAME + "message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
