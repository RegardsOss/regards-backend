/**
 *
 */
package fr.cnes.regards.framework.modules.jobs.domain.converters;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Path converter utils
 *
 * @author Léo Mieulet
 *
 */

@Converter
public class PathConverter implements AttributeConverter<Path, String> {

    @Override
    public String convertToDatabaseColumn(final Path pAttribute) {
        if (pAttribute == null) {
            return "";
        }
        return pAttribute.toString();
    }

    @Override
    public Path convertToEntityAttribute(final String pDbData) {
        return Paths.get(pDbData);
    }

}