/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.urn.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;

/**
 * Converter used by Hibernate (see AbstractEntity)
 * @author Sylvain Vissiere-Guerinet
 */
@Converter(autoApply = true)
public class OaisUrnConverter implements AttributeConverter<OaisUniformResourceName, String> {

    @Override
    public String convertToDatabaseColumn(OaisUniformResourceName pAttribute) {
        if (pAttribute == null) {
            return null;
        }
        return pAttribute.toString();
    }

    @Override
    public OaisUniformResourceName convertToEntityAttribute(String pDbData) {
        if (pDbData == null) {
            return new OaisUniformResourceName();
        }
        return OaisUniformResourceName.fromString(pDbData);
    }

}
