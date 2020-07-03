package fr.cnes.regards.modules.indexer.dao.mapping;

import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.domain.event.AbstractAttributeModelEvent;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

import java.util.Map;

public class AttributeDescription {

    private final String path;
    private final PropertyType type;
    private final RestrictionType restriction;
    private final Map<String, String> attributeProperties;

    public AttributeDescription(String path, PropertyType type, RestrictionType restriction,
            Map<String, String> attributeProperties) {
        this.path = path;
        this.type = type;
        this.restriction = restriction;
        this.attributeProperties = attributeProperties;
    }

    public AttributeDescription(AbstractAttributeModelEvent event) {
        this.path = event.getFullJsonPath();
        this.type = event.getPropertyType();
        this.restriction = event.getRestrictionType();
        this.attributeProperties = event.getAttributeProperties();
    }

    public String getPath() {
        return path;
    }

    public PropertyType getType() {
        return type;
    }

    public RestrictionType getRestriction() {
        return restriction;
    }

    public Map<String, String> getAttributeProperties() {
        return attributeProperties;
    }
}
