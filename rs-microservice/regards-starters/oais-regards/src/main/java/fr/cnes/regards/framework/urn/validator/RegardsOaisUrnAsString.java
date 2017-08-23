package fr.cnes.regards.framework.urn.validator;

import javax.validation.Constraint;
import java.lang.annotation.*;

import fr.cnes.regards.framework.urn.UniformResourceName;

/**
 * Annotation allowing to certifate that a String is a {@link UniformResourceName} thanks to {@link RegardsOaisUrnAsStringValidator}
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Constraint(validatedBy = RegardsOaisUrnAsStringValidator.class)
@Documented
public @interface RegardsOaisUrnAsString {

}
