package fr.cnes.regards.framework.oais.urn.validator;

import javax.validation.Constraint;

import fr.cnes.regards.framework.oais.urn.UniformResourceName;

import java.lang.annotation.*;

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
