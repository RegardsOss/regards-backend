/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.modules.core.validation;

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
 * @author svissier
 *
 */
public @interface PastOrNow {

    String message() default "{fr.cnes.modules.core.validation.PastOrNow." + "message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
