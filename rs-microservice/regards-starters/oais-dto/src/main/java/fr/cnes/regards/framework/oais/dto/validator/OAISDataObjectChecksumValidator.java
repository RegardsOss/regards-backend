/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.dto.validator;

import fr.cnes.regards.framework.oais.dto.OAISDataObjectDto;
import fr.cnes.regards.validation.utils.ChecksumValidationUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator to ensure the md5 checksum  of a {@link OAISDataObjectDto} is well formatted.
 *
 * @author Thibaud Michaudel
 **/
public class OAISDataObjectChecksumValidator
    implements ConstraintValidator<ValidOAISDataObjectChecksum, OAISDataObjectDto> {

    @Override
    public boolean isValid(OAISDataObjectDto oaisDataObjectDto, ConstraintValidatorContext constraintValidatorContext) {
        return ChecksumValidationUtils.isValidChecksum(oaisDataObjectDto.getChecksum(),
                                                       oaisDataObjectDto.getAlgorithm());
    }
}
