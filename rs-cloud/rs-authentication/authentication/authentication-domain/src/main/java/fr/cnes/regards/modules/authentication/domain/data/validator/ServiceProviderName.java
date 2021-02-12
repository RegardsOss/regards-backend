package fr.cnes.regards.modules.authentication.domain.data.validator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ServiceProviderNameValidator.class)
@Target( { ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceProviderName {
    String message() default "Invalid Service Provider name";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
