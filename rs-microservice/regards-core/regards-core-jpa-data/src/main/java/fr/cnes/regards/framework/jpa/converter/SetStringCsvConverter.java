/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.converter;

import java.util.Set;
import java.util.StringJoiner;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import com.google.common.collect.Sets;

/**
 * Allow to convert a {@link Set} of String to a simple String which each value is separated by a comma
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Converter
public class SetStringCsvConverter implements AttributeConverter<Set<String>, String> {

    private static final String DELIMITER = ",";

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
