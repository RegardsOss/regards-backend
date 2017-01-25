/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugins.datastorage.domain.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * this validator checks the conformity to the following {@link Pattern} {@link #REGEX_TO_RESPECT}
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class FileSizeValidator implements ConstraintValidator<FileSize, String> {

    /**
     * ^\\d+\\s*([kmgtpezy]i?)?(b|o|bits|bytes)$
     */
    public static final String REGEX_TO_RESPECT = "^\\d+\\s*([kmgtpezy]i?)?(b|o|bits|bytes)$";

    @Override
    public void initialize(FileSize pConstraintAnnotation) {
        // nothing to initialize
    }

    @Override
    public boolean isValid(String pValue, ConstraintValidatorContext pContext) {
        return (pValue == null) || respectRegex(pValue);
    }

    /**
     * @param pValue
     *            String to validate
     * @return true if and only if pValue matches {@link #REGEX_TO_RESPECT} case insensitively
     */
    private boolean respectRegex(String pValue) {
        Pattern pattern = Pattern.compile(REGEX_TO_RESPECT, Pattern.CASE_INSENSITIVE);
        return pattern.matcher(pValue).matches();
    }

}
