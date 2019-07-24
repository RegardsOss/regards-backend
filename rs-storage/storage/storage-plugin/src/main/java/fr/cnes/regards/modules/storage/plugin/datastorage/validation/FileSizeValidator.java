/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.storage.plugin.datastorage.validation;

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
