/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.model.domain.attributes.restriction;

import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.model.domain.schema.Restriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.util.CollectionUtils;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.HashSet;
import java.util.Set;

/**
 * Restriction for {@link PropertyType#JSON} to add jsonSchema validation
 *
 * @author Sébastien Binda
 */
@Entity
@DiscriminatorValue("JSON_SCHEMA")
public class JsonSchemaRestriction extends AbstractRestriction {

    @Column(name = "json_schema")
    private String jsonSchema;

    /**
     * Jsonified list of Strings
     **/
    @Column(name = "indexable_fields", columnDefinition = "jsonb")
    @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> indexableFields;

    public JsonSchemaRestriction() {
        this.type = RestrictionType.JSON_SCHEMA;
    }

    @Override
    public Boolean supports(PropertyType pAttributeType) {
        return pAttributeType == PropertyType.JSON;
    }

    @Override
    public Restriction toXml() {
        Restriction restriction = new Restriction();
        restriction.setJsonSchema(jsonSchema);
        if (!CollectionUtils.isEmpty(indexableFields)) {
            restriction.getIndexableField().addAll(indexableFields);
        }
        return restriction;
    }

    @Override
    public void fromXml(Restriction pXmlElement) {
        jsonSchema = pXmlElement.getJsonSchema();
        indexableFields = new HashSet<>(pXmlElement.getIndexableField());
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

    public Set<String> getIndexableFields() {
        if (this.indexableFields == null) {
            this.indexableFields = new HashSet<>();
        }
        return this.indexableFields;
    }
}
