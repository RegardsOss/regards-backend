/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.validation;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class DirectoryValidator implements ConstraintValidator<Directory, Path> {

    @Override
    public void initialize(Directory pConstraintAnnotation) {
        // nothing to initialize

    }

    @Override
    public boolean isValid(Path pValue, ConstraintValidatorContext pContext) {
        return (pValue == null) || Files.isDirectory(pValue);
    }

}
