/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * Class RoleAuthorizedAdressesConverter
 *
 * Convert from List<String> to String for database access
 *
 * @author Sébastien Binda

 */
@Converter
public class RoleAuthorizedAdressesConverter implements AttributeConverter<List<String>, String> {

    /**
     * List of values split character
     */
    private static final String SPLIT_CAR = ";";

    @Override
    public String convertToDatabaseColumn(final List<String> pValues) {
        String result = null;
        final StringBuilder builder = new StringBuilder();
        if ((pValues != null) && !pValues.isEmpty()) {
            for (final String value : pValues) {
                if (!builder.toString().isEmpty()) {
                    builder.append(SPLIT_CAR);
                }
                builder.append(value);
            }
            result = builder.toString();
        }
        return result;
    }

    @Override
    public List<String> convertToEntityAttribute(final String pValue) {
        final List<String> result = new ArrayList<>();
        if (pValue != null) {
            final String[] listOfValues = pValue.split(SPLIT_CAR);
            if (listOfValues.length > 0) {
                Collections.addAll(result, listOfValues);
            }
        }
        return result;
    }

}
