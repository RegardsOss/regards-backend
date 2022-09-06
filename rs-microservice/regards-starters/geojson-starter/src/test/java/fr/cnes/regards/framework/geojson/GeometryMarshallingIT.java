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
package fr.cnes.regards.framework.geojson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.geojson.deserializers.GeometryDeserializerModule;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

/**
 * @author Stephane Cortine
 **/
@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class GeometryMarshallingIT {

    @Autowired
    private Gson gson;

    @Autowired
    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_ABSENT);
        objectMapper.registerModule(new SimpleModule() {

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new BeanSerializerModifier() {

                    @Override
                    public JsonSerializer<?> modifySerializer(SerializationConfig config,
                                                              BeanDescription desc,
                                                              JsonSerializer<?> serializer) {
                        if (IGeometry.class.isAssignableFrom(desc.getBeanClass())) {
                            return new GeometryCustomSerializer((JsonSerializer<Object>) serializer);
                        }
                        return serializer;
                    }
                });
            }
        });
        objectMapper.registerModule(new GeometryDeserializerModule());
    }

    @Test
    public void featureWithoutGeometry() throws JsonProcessingException {
        // Given
        String id = "myId";

        Feature feature = new Feature();
        feature.setId(id);

        // When, then
        checkMarshallingFeature(feature);
    }

    @Test
    public void unlocated() throws JsonProcessingException {
        checkMarshallingGeometry(IGeometry.unlocated());
    }

    @Test
    public void point() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createPoint());
    }

    @Test
    public void multipoint() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createMultiPoint());
    }

    @Test
    public void linestring() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createLineString());
    }

    @Test
    public void multilinestring() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createMultiLineString());
    }

    @Test
    public void polygon() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createPolygon());
    }

    @Test
    public void multiPolygon() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createMultiPolygon());
    }

    @Test
    public void geometryCollection() throws JsonProcessingException {
        checkMarshallingGeometry(GeometryFactory.createGeometryCollection());
    }

    private void checkMarshallingGeometry(IGeometry geometry) throws JsonProcessingException {
        // Given
        Feature feature = new Feature();
        feature.setGeometry(geometry);

        // When, then
        checkMarshallingFeature(feature);
    }

    private void checkMarshallingFeature(Feature feature) throws JsonProcessingException {
        // Given
        // When
        // Serialize Gson -> deserialize Jackson
        String featureStringGson = gson.toJson(feature);

        Feature featureJackson = objectMapper.readValue(featureStringGson, Feature.class);

        Assert.assertNotNull(featureJackson.getGeometry());

        // Serialize Jackson -> deserialize Gson
        String featureStringJackson = objectMapper.writeValueAsString(feature);

        Feature featureGson = gson.fromJson(featureStringJackson, Feature.class);

        Assert.assertNotNull(featureGson.getGeometry());

        Map<String, Object> mapGson = gson.fromJson(featureStringGson, new TypeToken<Map<String, Object>>() {

        }.getType());
        Map<String, Object> mapJackson = objectMapper.readValue(featureStringJackson,
                                                                new TypeReference<Map<String, Object>>() {

                                                                });

        MapDifference<String, Object> diffMap = Maps.difference(mapGson, mapJackson);
        Map<String, MapDifference.ValueDifference<Object>> entriesDiffering = diffMap.entriesDiffering();

        // Then
        Assert.assertEquals(feature, featureGson);
        Assert.assertEquals(feature, featureJackson);

        Assert.assertTrue("The provided JSONs are different (difference number=" + entriesDiffering.size() + ")",
                          diffMap.areEqual());
    }

    /**
     * Custom serialzer for Jackson
     */
    static class GeometryCustomSerializer extends JsonSerializer<IGeometry> {

        private final JsonSerializer<Object> defaultSerializer;

        public GeometryCustomSerializer(JsonSerializer<Object> serializer) {
            defaultSerializer = serializer;
        }

        @Override
        public void serialize(IGeometry geometry, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
            if (geometry.getType() == GeoJsonType.UNLOCATED) {
                jsonGenerator.writeObject(null);
            } else {
                defaultSerializer.serialize(geometry, jsonGenerator, serializerProvider);
            }
        }
    }
}
