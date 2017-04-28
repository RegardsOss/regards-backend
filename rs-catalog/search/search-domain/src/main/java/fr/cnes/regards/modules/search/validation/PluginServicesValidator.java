/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.modules.search.domain.IService;

/**
 * Validator enforcing {@link PluginServices} constraints
 *
 * @author Sylvain Vissiere-Guerinet
 */
public class PluginServicesValidator implements ConstraintValidator<PluginServices, PluginConfiguration> {

    @Override
    public void initialize(PluginServices pConstraintAnnotation) {
        // nothing to do
    }

    @Override
    public boolean isValid(PluginConfiguration pValue, ConstraintValidatorContext pContext) {
        return (pValue != null) && pValue.getInterfaceNames().contains(IService.class.getName());
    }
}
