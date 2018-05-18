/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.util.Set;
import java.util.StringJoiner;

import com.google.common.collect.Sets;

/**
 * Allow to convert a {@link Set} of String to a simple String which each value is separated by a comma
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Converter
public class SetStringCsvConverter implements AttributeConverter<Set<String>, String> {

    /**
     * Delimiter used
     */
    private static final String DELIMITER = ",";

    /**
     * @param pSet
     * @return converted set to string for the database
     */
    @Override
    public String convertToDatabaseColumn(Set<String> pSet) {
        if (pSet == null) {
            return null;
        }
        StringJoiner sj = new StringJoiner(DELIMITER);
        for (String entry : pSet) {
            sj.add(entry);
        }
        return sj.toString();

    }

    /**
     * @param pArg0
     * @return converted string from database to a set
     */
    @Override
    public Set<String> convertToEntityAttribute(String pArg0) {
        Set<String> result = Sets.newHashSet();
        if (pArg0 == null) {
            return result;
        }
        String[] entries = pArg0.split(DELIMITER);
        for (String entry : entries) {
            result.add(entry);
        }
        return result;
    }

}
