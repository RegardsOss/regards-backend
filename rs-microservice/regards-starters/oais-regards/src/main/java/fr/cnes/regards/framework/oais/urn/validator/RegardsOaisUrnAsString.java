package fr.cnes.regards.framework.oais.urn.validator;

import javax.validation.Constraint;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * Annotation allowing to certifate that a String is a {@link UniformResourceName} thanks to
 * {@link RegardsOaisUrnAsStringValidator}
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Constraint(validatedBy = RegardsOaisUrnAsStringValidator.class)
@Documented
public @interface RegardsOaisUrnAsString {

}
