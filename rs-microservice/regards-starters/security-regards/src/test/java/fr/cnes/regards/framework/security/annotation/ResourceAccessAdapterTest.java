/*
 * LICENSE_PLACEHOLDER
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

/**
 *
 * Class ResourceAccessAdapterTest
 *
 * Test to check json serialization/deserialization for ResourceAccess annotation
 *
 * @author sbinda
 * @since 1.0-SNAPSHOT
 */
public class ResourceAccessAdapterTest {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceAccessAdapterTest.class);

    /**
     *
     * Test to check json serialization/deserialization for ResourceAccess annotation
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testResourceJsonAdapter() {

        final StringWriter swriter = new StringWriter();
        final JsonWriter writer = new JsonWriter(swriter);
        final ResourceAccessAdapter adapter = new ResourceAccessAdapter();
        final String jsonResourceAccess = "{\"name\":\"name\",\"description\":\"description\"}";

        // Initiate test ResourceAccess to serialize
        final Map<String, Object> attributs = new HashMap<>();
        attributs.put("name", "name");
        attributs.put("description", "description");
        final ResourceAccess resourceAccess = AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class,
                                                                                   null);

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
            Assert.assertTrue(resource.name().equals(resourceAccess.name()));
            Assert.assertTrue(resource.description().equals(resourceAccess.description()));
        } catch (final IOException e) {
            Assert.fail(e.getMessage());
        }

    }

}
