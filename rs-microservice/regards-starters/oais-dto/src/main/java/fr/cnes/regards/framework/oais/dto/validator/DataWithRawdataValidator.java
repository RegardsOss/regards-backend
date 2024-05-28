/*

 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.framework.oais.dto.validator;

import fr.cnes.regards.framework.oais.dto.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.dto.ContentInformationDto;
import fr.cnes.regards.framework.oais.dto.InformationPackageProperties;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DataWithRawdataValidator implements ConstraintValidator<DataWithRawdata, AbstractInformationPackage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataWithRawdataValidator.class);

    @Override
    public void initialize(DataWithRawdata constraintAnnotation) {
        // Nothing to do
    }

    @Override
    public boolean isValid(AbstractInformationPackage value, ConstraintValidatorContext context) {
        // Only DATA has special validation
        if (value.getIpType() == EntityType.DATA) {
            InformationPackageProperties properties = (InformationPackageProperties) value.getProperties();
            if (properties == null) {
                // because of SIP which are references
                return true;
            }

            for (ContentInformationDto ci : properties.getContentInformations()) {
                if (ci == null) {
                    LOGGER.error("Null content information detected, JSON file may contain unnecessary commas!");
                    return false;
                }
                if (ci.getDataObject().getRegardsDataType() == DataType.RAWDATA) {
                    // At least one raw data is detected
                    return true;
                }
            }
            return false;
        }

        return true;
    }

}
