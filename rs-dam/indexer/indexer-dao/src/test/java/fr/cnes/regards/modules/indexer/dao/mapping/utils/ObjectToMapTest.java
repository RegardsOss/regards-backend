/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Julien Canches
 */
class ObjectToMapTest {

    private final Gson gson = new Gson();

    record Simple(String string,
                  int integer,
                  long longValue,
                  float floatValue,
                  double doubleValue,
                  boolean boolValue) {

    }

    record Nested(String id,
                  Simple details) {

    }

    record WithList(String title,
                    List<Integer> numbers) {

    }

    @Test
    void testSimpleObjectConversion() {
        // GIVEN
        Simple s = new Simple("str1", 30, -12L, 3f, 5d, true);
        // WHEN
        Map<String, Object> map = ObjectToMap.toMap(gson, s);
        // THEN
        assertEquals("str1", map.get("string"));
        assertEquals(30L, map.get("integer"));
        assertEquals(-12L, map.get("longValue"));
        assertEquals(3f, map.get("floatValue"));
        assertEquals(5d, map.get("doubleValue"));
        assertEquals(30L, map.get("integer"));
        assertEquals(true, map.get("boolValue"));
    }

    @Test
    void testNestedObjectConversion() {
        // GIVEN
        Nested n = new Nested("123", new Simple("str2", 25, Long.MAX_VALUE, 0f, 0d, false));
        // WHEN
        Map<String, Object> map = ObjectToMap.toMap(gson, n);
        // THEN
        assertEquals("123", map.get("id"));
        @SuppressWarnings("unchecked") Map<String, Object> details = (Map<String, Object>) map.get("details");
        assertEquals("str2", details.get("string"));
        assertEquals(25L, details.get("integer"));
        assertEquals(false, details.get("boolValue"));
    }

    @Test
    void testListConversion() {
        // GIVEN
        WithList obj = new WithList("Numbers", List.of(1, 2, 3));
        // WHEN
        Map<String, Object> map = ObjectToMap.toMap(gson, obj);
        // THEN
        assertEquals("Numbers", map.get("title"));
        @SuppressWarnings("unchecked") List<Object> numbers = (List<Object>) map.get("numbers");
        assertEquals(List.of(1L, 2L, 3L), numbers);
    }

    @Test
    void testNullValues() {
        // GIVEN
        Simple s = new Simple(null, 0, 0L, 0f, 0d, false);
        // WHEN
        Map<String, Object> map = ObjectToMap.toMap(gson, s);
        // THEN
        assertNull(map.get("string"));
        assertEquals(0L, map.get("integer"));
        assertEquals(false, map.get("boolValue"));
    }

    static List<Object> testPrimitiveThrowsException() {
        return List.of(123, "text", true);
    }

    @ParameterizedTest
    @MethodSource
    void testPrimitiveThrowsException(Object primitiveValue) {
        assertThrows(IllegalArgumentException.class, () -> ObjectToMap.toMap(gson, primitiveValue));
    }

    @Test
    void testComplexNestedStructure() {
        // GIVEN
        Map<String, Object> src = Map.of("user",
                                         new Simple("Eve", 40, 80L, 3.14f, Math.PI, true),
                                         "tags",
                                         List.of("alpha", "beta"));
        // WHEN
        Map<String, Object> map = ObjectToMap.toMap(gson, src);
        // THEN
        @SuppressWarnings("unchecked") Map<String, Object> user = (Map<String, Object>) map.get("user");
        assertEquals("Eve", user.get("string"));
        assertEquals(40L, user.get("integer"));
        assertEquals(true, user.get("boolValue"));
        @SuppressWarnings("unchecked") List<Object> tags = (List<Object>) map.get("tags");
        assertEquals(List.of("alpha", "beta"), tags);
    }

    @Test
    void testJsonDocument() {
        JsonObject obj = gson.fromJson("""
                                           {
                                                   "street_address" : "ici",
                                                   "code" : 31,
                                                   "active" : true,
                                                   "price" : 10.52,
                                                   "dates" : [ "2021-01-25T15:14:14.694Z" ]
                                           }""", JsonObject.class);
        // WHEN
        Map<String, Object> map = ObjectToMap.toMap(gson, obj);
        assertEquals("ici", map.get("street_address"));
        assertEquals(31L, map.get("code"));
        assertEquals(true, map.get("active"));
        assertEquals(10.52, map.get("price"));
        assertEquals(List.of("2021-01-25T15:14:14.694Z"), map.get("dates"));
    }

}
