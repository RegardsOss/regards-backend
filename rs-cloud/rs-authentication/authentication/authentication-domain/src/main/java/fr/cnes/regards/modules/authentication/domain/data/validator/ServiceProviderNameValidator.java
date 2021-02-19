package fr.cnes.regards.modules.authentication.domain.data.validator;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.net.MalformedURLException;
import java.net.URL;

public class ServiceProviderNameValidator implements ConstraintValidator<ServiceProviderName, String> {

    @Override
    public boolean isValid(String name, ConstraintValidatorContext context) {
        try {
            new URL("https://regardsoss.github.io/" + name);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
