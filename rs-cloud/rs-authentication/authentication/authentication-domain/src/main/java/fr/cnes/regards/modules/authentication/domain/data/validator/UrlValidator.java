package fr.cnes.regards.modules.authentication.domain.data.validator;

import org.hibernate.validator.constraints.URL;
import org.hibernate.validator.internal.constraintvalidators.hv.URLValidator;
import org.springframework.core.annotation.AnnotationUtils;

import javax.validation.ConstraintValidatorContext;

public final class UrlValidator {

    private UrlValidator() {}

    private final static URLValidator urlValidator = new URLValidator();
    {
        try {
            URL url = AnnotationUtils.findAnnotation(Class.forName(URL.class.getName()), URL.class);
            urlValidator.initialize(url);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isValid(String url, ConstraintValidatorContext context) {
        return urlValidator.isValid(url, context);
    }
}
