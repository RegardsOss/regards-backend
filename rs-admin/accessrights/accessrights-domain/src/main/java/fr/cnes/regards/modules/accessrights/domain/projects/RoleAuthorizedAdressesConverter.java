/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 *
 * Class RoleAuthorizedAdressesConverter
 *
 * Convert from List<String> to String for database access
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@Converter
public class RoleAuthorizedAdressesConverter implements AttributeConverter<List<String>, String> {

    /**
     * List of values split character
     */
    private static final String SPLIT_CAR = ";";

    @Override
    public String convertToDatabaseColumn(final List<String> pValues) {
        String result = "";
        for (final String value : pValues) {
            result += value + SPLIT_CAR;
        }
        return result;
    }

    @Override
    public List<String> convertToEntityAttribute(final String pValue) {
        final List<String> result = new ArrayList<>();
        final String[] listOfValues = pValue.split(SPLIT_CAR);
        if (listOfValues.length > 0) {
            for (final String value : listOfValues) {
                result.add(value);
            }
        }
        return result;
    }

}
