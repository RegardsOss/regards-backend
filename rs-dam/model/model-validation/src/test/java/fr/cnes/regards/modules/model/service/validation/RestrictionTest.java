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
package fr.cnes.regards.modules.model.service.validation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.modules.model.domain.attributes.restriction.JsonSchemaRestriction;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.service.validation.validator.restriction.RestrictionValidatorFactory;

/**
 *
 * @author Sébastien Binda
 *
 */
public class RestrictionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestrictionTest.class);

    @Test
    public void testValid() throws IOException {

        JsonSchemaRestriction restriction = new JsonSchemaRestriction();
        InputStream in = Files.newInputStream(Paths.get("src", "test", "resources", "schema.json"));
        String schema = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        InputStream inTest = Files.newInputStream(Paths.get("src", "test", "resources", "valid.json"));
        JsonObject json = (new GsonBuilder()).create().fromJson(new JsonReader(new InputStreamReader(inTest)),
                                                                JsonObject.class);
        restriction.setJsonSchema(schema);
        Errors errors = new MapBindingResult(new HashMap<>(), "test");
        RestrictionValidatorFactory.getValidator(restriction, "test").validate(IProperty.buildJson("test", json),
                                                                               errors);
        errors.getAllErrors().forEach(e -> {
            LOGGER.error(e.getDefaultMessage());
        });
        Assert.assertFalse("", errors.hasErrors());
    }

    @Test
    public void testInvalidDate() throws IOException {

        JsonSchemaRestriction restriction = new JsonSchemaRestriction();
        InputStream in = Files.newInputStream(Paths.get("src", "test", "resources", "schema.json"));
        String schema = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        InputStream inTest = Files.newInputStream(Paths.get("src", "test", "resources", "invalid.json"));
        JsonObject json = (new GsonBuilder()).create().fromJson(new JsonReader(new InputStreamReader(inTest)),
                                                                JsonObject.class);
        restriction.setJsonSchema(schema);
        Errors errors = new MapBindingResult(new HashMap<>(), "test");
        RestrictionValidatorFactory.getValidator(restriction, "test").validate(IProperty.buildJson("test", json),
                                                                               errors);
        errors.getAllErrors().forEach(e -> {
            LOGGER.error(e.getDefaultMessage());
        });
        Assert.assertFalse("", errors.hasErrors());
    }

    @Test
    public void testInvalidSchema() throws IOException {

        JsonSchemaRestriction restriction = new JsonSchemaRestriction();
        InputStream in = Files.newInputStream(Paths.get("src", "test", "resources", "invalid-schema.json"));
        String schema = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        InputStream inTest = Files.newInputStream(Paths.get("src", "test", "resources", "valid.json"));
        JsonObject json = (new GsonBuilder()).create().fromJson(new JsonReader(new InputStreamReader(inTest)),
                                                                JsonObject.class);
        restriction.setJsonSchema(schema);
        Errors errors = new MapBindingResult(new HashMap<>(), "test");
        RestrictionValidatorFactory.getValidator(restriction, "test").validate(IProperty.buildJson("test", json),
                                                                               errors);
        errors.getAllErrors().forEach(e -> {
            LOGGER.error(e.getDefaultMessage());
        });
        Assert.assertTrue("", errors.hasErrors());
    }

    @Test
    public void testInvalidSchema2() throws IOException {

        JsonSchemaRestriction restriction = new JsonSchemaRestriction();
        InputStream in = Files.newInputStream(Paths.get("src", "test", "resources", "invalid-schema-2.json"));
        String schema = IOUtils.toString(in, StandardCharsets.UTF_8.name());

        InputStream inTest = Files.newInputStream(Paths.get("src", "test", "resources", "valid.json"));
        JsonObject json = (new GsonBuilder()).create().fromJson(new JsonReader(new InputStreamReader(inTest)),
                                                                JsonObject.class);
        restriction.setJsonSchema(schema);
        Errors errors = new MapBindingResult(new HashMap<>(), "test");
        RestrictionValidatorFactory.getValidator(restriction, "test").validate(IProperty.buildJson("test", json),
                                                                               errors);
        errors.getAllErrors().forEach(e -> {
            LOGGER.error(e.getDefaultMessage());
        });
        Assert.assertTrue("", errors.hasErrors());
    }

}
