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
package fr.cnes.regards.modules.model.gson;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.IOUtils;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

/**
 *
 * @author Sébastien Binda
 *
 */
public class TestAttributeHelper extends AbstractAttributeHelper {

    @Override
    public Set<AttributeModel> getAllCommonAttributes(Collection<String> modelNames) throws ModuleException {
        return Sets.newHashSet();
    }

    @Override
    protected List<AttributeModel> doGetAllAttributes(String tenant) {
        List<AttributeModel> atts = Lists.newArrayList();
        AttributeModel att = new AttributeModel();
        att.setName("test_json");
        att.setJsonPath("properties.test_json");
        att.setLabel("test");
        att.setType(PropertyType.JSON);
        try {
            JsonSchemaRestriction restriction = new JsonSchemaRestriction();
            InputStream in = Files.newInputStream(Paths.get("src", "test", "resources", "schema.json"));
            String schema = IOUtils.toString(in, StandardCharsets.UTF_8.name());
            restriction.setJsonSchema(schema);
            att.setRestriction(restriction);
            atts.add(att);
        } catch (IOException e) {
            // Nothing to do
        }
        return atts;
    }

}
