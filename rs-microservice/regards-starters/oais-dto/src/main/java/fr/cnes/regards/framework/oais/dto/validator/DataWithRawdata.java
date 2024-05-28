package fr.cnes.regards.framework.oais.dto.validator;

import fr.cnes.regards.framework.oais.dto.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectDto;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Ensure that {@link AbstractInformationPackage} of type {@link EntityType#DATA} have at least one
 * {@link OAISDataObjectDto} of type {@link DataType#RAWDATA}
 *
 * @author Sylvain Vissiere-Guerinet
 * <p>
 * FIXME removed from {@link AbstractInformationPackage} because an AIP might not have any RAWDATA, class to remove with its validator.
 */
@Target({ ElementType.TYPE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = { DataWithRawdataValidator.class })
public @interface DataWithRawdata {

    /**
     * @return error message key
     */
    String message() default "Information package of type DATA must have at least one file of type RAWDATA.";

    /**
     * @return validation groups
     */
    Class<?>[] groups() default {};

    /**
     * @return custom payload
     */
    Class<? extends Payload>[] payload() default {};
}
