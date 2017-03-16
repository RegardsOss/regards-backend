/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.domain.IConverter;

/**
 *
 * Assure that annotated {@link PluginConfiguration} is a plugin configuration of an {@link IConverter} plugin
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Constraint(validatedBy = PluginConvertersValidator.class)
@Documented
public @interface PluginConverters {

    /**
     * Class to validate
     */
    static final String CLASS_NAME = "fr.cnes.regards.modules.search.validation.PluginConverters.";

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
