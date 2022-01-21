/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.testutils;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import fr.cnes.regards.modules.processing.utils.gson.GsonProcessingTestUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static fr.cnes.regards.modules.processing.utils.random.RandomUtils.randomInstance;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base marshalling/unmarshalling test (for DTOs, etc.).
 *
 * @author gandrieu
 */
public abstract class AbstractMarshallingTest<T> {

    public abstract Class<T> testedType();

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMarshallingTest.class);

    private final Gson gson = GsonProcessingTestUtils.gson();

    @Test
    public void test_toJson_fromJson() {
        Class<T> testedType = testedType();
        for (int i = 0; i < 100 ; i++) {
            T expected = randomInstance(testedType);
            String expectedJson = gson.toJson(expected);
            T actual = gson.fromJson(expectedJson, testedType);
            String actualJson = gson.toJson(actual);
            boolean equal = actualJson.equals(expectedJson);
            // The string cannot be compared, as the order of JSON attributes are not assured
            // so we transform it into Map to compare it recursively
            Map<Object, Object> resMap = gson.fromJson(expectedJson, new TypeToken<Map<Object, Object>>() {}.getType());
            Map<Object, Object> expectedMap = gson.fromJson(actualJson, new TypeToken<Map<Object, Object>>() {}.getType());
            assertThat(resMap).usingRecursiveComparison().isEqualTo(expectedMap);
        }
    }

}
