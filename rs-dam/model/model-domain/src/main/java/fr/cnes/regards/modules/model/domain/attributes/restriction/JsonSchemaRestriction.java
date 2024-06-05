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

import fr.cnes.regards.framework.jpa.json.JsonBinaryType;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import fr.cnes.regards.framework.jpa.json.JsonTypeDescriptor;
import fr.cnes.regards.modules.model.domain.schema.Restriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.springframework.util.CollectionUtils;

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
    @Type(value = JsonBinaryType.class,
          parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "java.lang.String") })
    private Set<String> indexableFields;

    public JsonSchemaRestriction() {
        this.type = RestrictionType.JSON_SCHEMA;
    }

    @Override
    public Boolean supports(PropertyType attributeType) {
        return attributeType == PropertyType.JSON;
    }

    @Override
    public Boolean validate() {
        // Json schema must be empty "{}" or containing at least one valid validator.
        if (getJsonSchema() != null && !getJsonSchema().trim().equals("{}")) {
            return !JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7)
                                     .getSchema(getJsonSchema())
                                     .getValidators()
                                     .isEmpty();
        } else {
            return Boolean.TRUE;
        }
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
    public void fromXml(Restriction xmlElement) {
        jsonSchema = xmlElement.getJsonSchema();
        indexableFields = new HashSet<>(xmlElement.getIndexableField());
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
