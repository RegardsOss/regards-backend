package fr.cnes.regards.modules.authentication.domain.data.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ServiceProviderNameValidator implements ConstraintValidator<ServiceProviderName, String> {

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        return UrlValidator.isValid("https://regardsoss.github.io/" + name, context);
    }
}
