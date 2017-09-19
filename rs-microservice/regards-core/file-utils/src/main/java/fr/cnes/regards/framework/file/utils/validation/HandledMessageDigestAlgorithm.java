package fr.cnes.regards.framework.file.utils.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.security.MessageDigest;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Verify thanks to {@link HandledMessageDigestAlgorithmValidator} that the annotated field or parameter represents
 * a handled algorithm by the current jvm.
 * <br/>
 * See for more information {@link MessageDigest}
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { HandledMessageDigestAlgorithmValidator.class })
public @interface HandledMessageDigestAlgorithm {

    /**
     * Class to validate
     */
    String CLASS_NAME = "fr.cnes.regards.framework.file.utils.validation.HandledMessageDigestAlgorithm";

    /**
     * @return error message key
     */
    String message() default "{Validation annotation @" + CLASS_NAME
            + " validating %s: it is not an handled algorithm for checksum computation";

    /**
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * @return custom payload
     */
    Class<? extends Payload>[] payload() default {};
}
