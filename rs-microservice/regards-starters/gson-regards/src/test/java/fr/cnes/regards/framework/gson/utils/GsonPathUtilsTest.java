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
package fr.cnes.regards.framework.gson.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.PathNotFoundException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A couple of tests for GsonPathUtils.
 * They are in no way exhaustive because most of the hard work is done by jayway's JsonPath and this library has its
 * own extensive tests.
 *
 * @author Julien Canches
 */
class GsonPathUtilsTest {

    static List<Arguments> getStringSuccess() {
        return List.of(Arguments.of("{ \"a\": \"b\" }", "$.a", "b"),
                       Arguments.of("{ \"a\": { \"b\" : 2 } }", "$.a.b", "2"),
                       Arguments.of("{ \"a\": { \"b\" : [ 2, 3 ] } }", "$.a.b[1]", "3"));
    }

    @ParameterizedTest
    @MethodSource
    void getStringSuccess(String input, String jsonPath, String expected) {
        // GIVEN
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        // WHEN
        String result = GsonPathUtils.getString(json, jsonPath);
        // THEN
        assertThat(result).isEqualTo(expected);
    }

    static List<Arguments> getStringFailure() {
        return List.of(Arguments.of("{ \"a\": \"b\" }", "$.c", PathNotFoundException.class),
                       Arguments.of("{ \"a\": { \"b\" : [ 2, 3 ] } }", "$.a.b", IllegalStateException.class),
                       Arguments.of("{ \"a\": { \"b\" : { \"c\" : true } } }",
                                    "$.a.b",
                                    UnsupportedOperationException.class));
    }

    @ParameterizedTest
    @MethodSource
    void getStringFailure(String input, String jsonPath, Class<? extends Throwable> expectedExceptionType) {
        // GIVEN
        JsonObject json = JsonParser.parseString(input).getAsJsonObject();
        // WHEN/THEN
        assertThatThrownBy(() -> GsonPathUtils.getString(json, jsonPath)).isInstanceOf(expectedExceptionType);
    }

}
