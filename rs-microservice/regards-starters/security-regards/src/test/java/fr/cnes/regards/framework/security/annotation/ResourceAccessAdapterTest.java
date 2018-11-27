/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.annotation;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.security.role.DefaultRole;

/**
 * Class ResourceAccessAdapterTest
 *
 * Test to check json serialization/deserialization for ResourceAccess annotation
 * @author sbinda
 */
public class ResourceAccessAdapterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceAccessAdapterTest.class);

    /**
     * Test to check json serialization/deserialization for ResourceAccess annotation
     */
    @Test
    public void testResourceJsonAdapter() {

        final StringWriter swriter = new StringWriter();
        final JsonWriter writer = new JsonWriter(swriter);
        final ResourceAccessAdapter adapter = new ResourceAccessAdapter();
        final String jsonResourceAccess = "{\"role\":\"ADMIN\",\"description\":\"description\"}";

        // Initiate test ResourceAccess to serialize
        final Map<String, Object> attributs = new HashMap<>();
        attributs.put(ResourceAccessAdapter.ROLE_LABEL, DefaultRole.ADMIN);
        attributs.put(ResourceAccessAdapter.DESCRIPTION_LABEL, "description");
        final ResourceAccess resourceAccess = AnnotationUtils
                .synthesizeAnnotation(attributs, ResourceAccess.class, null);

        try {
            // Serialize test
            adapter.write(writer, resourceAccess);
            Assert.assertTrue("Invalid transformation to json for annotation REsourceAccess",
                              jsonResourceAccess.equals(swriter.toString()));
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        }

        try {
            // Deserialize test
            final ResourceAccess resource = adapter.read(new JsonReader(new StringReader(jsonResourceAccess)));
            Assert.assertNotNull(resource);
            Assert.assertTrue(resource.role().equals(resourceAccess.role()));
            Assert.assertTrue(resource.description().equals(resourceAccess.description()));
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        }

    }

}
