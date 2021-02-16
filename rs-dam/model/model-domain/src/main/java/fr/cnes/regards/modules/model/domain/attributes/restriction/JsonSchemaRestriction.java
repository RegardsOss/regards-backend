/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import fr.cnes.regards.modules.model.domain.schema.Restriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

/**
 *
 * @author Sébastien Binda
 *
 */
@Entity
@DiscriminatorValue("JSON-SCHEMA")
public class JsonSchemaRestriction extends AbstractRestriction {

    @Column(name = "json_schema")
    private String jsonSchema;

    public JsonSchemaRestriction() {
        this.type = RestrictionType.JSON_SHCEMA;
    }

    @Override
    public Boolean supports(PropertyType pAttributeType) {
        return pAttributeType == PropertyType.JSON;
    }

    @Override
    public Restriction toXml() {
        Restriction restriction = new Restriction();
        restriction.setJsonSchema(jsonSchema);
        return restriction;
    }

    @Override
    public void fromXml(Restriction pXmlElement) {
        jsonSchema = pXmlElement.getJsonSchema();
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public void setJsonSchema(String jsonSchema) {
        this.jsonSchema = jsonSchema;
    }

}
