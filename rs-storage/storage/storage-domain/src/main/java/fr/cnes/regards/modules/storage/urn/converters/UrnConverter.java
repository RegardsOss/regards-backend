/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.urn.converters;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import fr.cnes.regards.modules.storage.urn.UniformResourceName;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@Converter(autoApply = true)
public class UrnConverter implements AttributeConverter<UniformResourceName, String> {

    @Override
    public String convertToDatabaseColumn(UniformResourceName pAttribute) {
        if (pAttribute == null) {
            return null;
        }
        return pAttribute.toString();
    }

    @Override
    public UniformResourceName convertToEntityAttribute(String pDbData) {
        if (pDbData == null) {
            return new UniformResourceName();
        }
        return UniformResourceName.fromString(pDbData);
    }

}
