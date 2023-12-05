package fr.cnes.regards.framework.oais.dto.urn.validator;

import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;

import javax.validation.Constraint;
import java.lang.annotation.*;

/**
 * Annotation allowing to certifate that a String is a {@link OaisUniformResourceName} thanks to
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
