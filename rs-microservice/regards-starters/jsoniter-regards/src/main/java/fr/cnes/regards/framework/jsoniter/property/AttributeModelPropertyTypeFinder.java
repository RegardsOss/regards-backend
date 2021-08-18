package fr.cnes.regards.framework.jsoniter.property;

import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import io.vavr.control.Option;

import java.util.List;

public interface AttributeModelPropertyTypeFinder {

    Option<PropertyType> getPropertyTypeForAttributeWithName(String name);

    default void refresh(String defaultTenant, List<AttributeModel> atts) {}
}
