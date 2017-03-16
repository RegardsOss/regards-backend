/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.domain.IFilter;

/**
 * Validator enforcing {@link PluginFilters} constraints
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class PluginFiltersValidator implements ConstraintValidator<PluginFilters, PluginConfiguration> {

    @Override
    public void initialize(PluginFilters pConstraintAnnotation) {
        // nothing to do
    }

    @Override
    public boolean isValid(PluginConfiguration pValue, ConstraintValidatorContext pContext) {
        return (pValue != null) && pValue.getInterfaceName().equals(IFilter.class.getName());
    }

}
