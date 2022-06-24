package fr.cnes.regards.modules.indexer.dao.mapping;

import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.domain.event.AbstractAttributeModelEvent;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

import java.util.Map;
import java.util.Objects;

public class AttributeDescription {

    private final String path;

    private final PropertyType type;

    private final RestrictionType restriction;

    private final Map<String, String> attributeProperties;

    private final String fixedMapping;

    private final boolean indexed;

    public AttributeDescription(String path,
                                PropertyType type,
                                RestrictionType restriction,
                                Map<String, String> attributeProperties,
                                String fixedMapping,
                                boolean indexed) {
        this.path = path;
        this.type = type;
        this.restriction = restriction;
        this.attributeProperties = attributeProperties;
        this.fixedMapping = fixedMapping;
        this.indexed = indexed;
    }

    public AttributeDescription(String path,
                                PropertyType type,
                                RestrictionType restriction,
                                Map<String, String> attributeProperties,
                                String fixedMapping) {
        this(path, type, restriction, attributeProperties, fixedMapping, false);
    }

    public AttributeDescription(AbstractAttributeModelEvent event) {
        this.path = event.getFullJsonPath();
        this.type = event.getPropertyType();
        this.restriction = event.getRestrictionType();
        this.attributeProperties = event.getAttributeProperties();
        this.fixedMapping = event.getEsMappping();
        this.indexed = event.isIndexed();
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

    public String getFixedMapping() {
        return fixedMapping;
    }

    public boolean isIndexed() {
        return indexed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        AttributeDescription that = (AttributeDescription) o;
        return path.equals(that.path) && (type == that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, type);
    }

}
