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
import fr.cnes.regards.modules.search.domain.IFilter;

/**
 *
 * Assure that annotated {@link PluginConfiguration} is a plugin configuration of an {@link IFilter} plugin
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Constraint(validatedBy = PluginFiltersValidator.class)
@Documented
public @interface PluginFilters {

    /**
     * Class to validate
     */
    static final String CLASS_NAME = "fr.cnes.regards.modules.search.validation.PluginFilters.";

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
