/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jpa.converter;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Collections;

/**
 * @author Léo Mieulet
 *
 */
@Converter(autoApply = true)
public class MimeTypeConverter implements AttributeConverter<MimeType, String> {

    @Override
    public String convertToDatabaseColumn(MimeType mimeType) {
        if (mimeType == null) {
            return null;
        }
        return mimeType.toString();
    }

    @Override
    public MimeType convertToEntityAttribute(String mimeTypeAsString) {
        if (mimeTypeAsString == null) {
            return null;
        }
        return MediaType.valueOf(mimeTypeAsString);
    }

}
