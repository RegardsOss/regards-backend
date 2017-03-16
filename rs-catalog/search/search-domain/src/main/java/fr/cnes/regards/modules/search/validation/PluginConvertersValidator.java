/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.domain.IConverter;

/**
 * Validator enforcing {@link PluginConverters} constraints
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class PluginConvertersValidator implements ConstraintValidator<PluginConverters, PluginConfiguration> {

    @Override
    public void initialize(PluginConverters pConstraintAnnotation) {
        // nothing to do
    }

    @Override
    public boolean isValid(PluginConfiguration pValue, ConstraintValidatorContext pContext) {
        return (pValue != null) && pValue.getInterfaceName().equals(IConverter.class.getName());
    }
}
