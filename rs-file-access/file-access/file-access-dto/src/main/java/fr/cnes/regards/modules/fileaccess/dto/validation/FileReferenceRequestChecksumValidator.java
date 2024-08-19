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
package fr.cnes.regards.modules.fileaccess.dto.validation;

import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator to ensure the md5 checksum  of a {@link fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto} is well formatted.
 *
 * @author Thibaud Michaudel
 **/
public class FileReferenceRequestChecksumValidator
    implements ConstraintValidator<ValidFileReferenceRequestChecksum, FileReferenceRequestDto> {

    @Override
    public boolean isValid(FileReferenceRequestDto fileReferenceRequestDto,
                           ConstraintValidatorContext constraintValidatorContext) {

        if (fileReferenceRequestDto.getAlgorithm() == null || fileReferenceRequestDto.getChecksum() == null) {
            return false;
        }
        if (fileReferenceRequestDto.getAlgorithm().equals("MD5")) {
            String md5pattern = "^[0-9a-fA-F]{32}$";
            return fileReferenceRequestDto.getChecksum().matches(md5pattern);
        }
        // Only MD5 checksum need to be validated in regards
        return true;
    }
}
