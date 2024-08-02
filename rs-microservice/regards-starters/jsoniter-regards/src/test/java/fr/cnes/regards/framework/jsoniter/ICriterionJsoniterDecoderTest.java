/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.jsoniter;

import com.jsoniter.JsonIterator;
import fr.cnes.regards.framework.jsoniter.decoders.ICriterionJsoniterDecoder;
import fr.cnes.regards.modules.indexer.domain.criterion.ComparisonOperator;
import fr.cnes.regards.modules.indexer.domain.criterion.RangeCriterion;
import fr.cnes.regards.modules.indexer.domain.criterion.ValueComparison;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

class ICriterionJsoniterDecoderTest {

    @Test
    void testSimpleRangeCrit() throws IOException {
        String content = """
            {
              "valueComparisons": [
                {
                  "operator": "GREATER",
                  "value": 10.0
                }
              ],
              "name": "feature.properties.IntegerA",
              "@type@": "fr.cnes.regards.modules.indexer.domain.criterion.RangeCriterion"
            }
            """;
        ICriterionJsoniterDecoder decoder = new ICriterionJsoniterDecoder();
        Object parsed = decoder.decode(JsonIterator.parse(content));
        if (parsed instanceof RangeCriterion<?> rangeCriterion) {
            List<? extends ValueComparison<?>> valueComparisons = rangeCriterion.getValueComparisons()
                                                                                .stream()
                                                                                .toList();
            Assertions.assertEquals(1, valueComparisons.size(), "There should be only 1 value comparison");
            Assertions.assertEquals(ComparisonOperator.GREATER,
                                    valueComparisons.get(0).getOperator(),
                                    "Operator should be GREATER");
            // we expect 10L because deserializer cannot differentiate between Integer and Long
            Assertions.assertEquals(10L, valueComparisons.get(0).getValue(), "Value should be 10");
            Assertions.assertEquals("feature.properties.IntegerA", rangeCriterion.getName());
        } else {
            Assertions.fail("Should be able to deserialize as RangeCriterion");
        }
    }

    @Test
    void testBetweenRangeCrit() throws IOException {
        String content = """
             {
             "valueComparisons":[
                 {
                  "operator":"GREATER",
                  "value":10.0
                 },
                 {
                  "operator":"LESS",
                  "value":12.0
                 }
               ],
              "name":"feature.properties.IntegerA",
              "@type@":"fr.cnes.regards.modules.indexer.domain.criterion.RangeCriterion"
              }
            """;
        ICriterionJsoniterDecoder decoder = new ICriterionJsoniterDecoder();
        Object parsed = decoder.decode(JsonIterator.parse(content));
        if (parsed instanceof RangeCriterion<?> rangeCriterion) {
            List<? extends ValueComparison<?>> valueComparisons = rangeCriterion.getValueComparisons()
                                                                                .stream()
                                                                                .sorted(Comparator.comparing(
                                                                                    ValueComparison::getOperator))
                                                                                .toList();
            Assertions.assertEquals(2, valueComparisons.size(), "There should be 2 value comparisons");
            Assertions.assertEquals(ComparisonOperator.GREATER,
                                    valueComparisons.get(0).getOperator(),
                                    "Operator should be GREATER");
            // we expect 10L because deserializer cannot differentiate between Integer and Long
            Assertions.assertEquals(10L, valueComparisons.get(0).getValue(), "Value should be 10");
            Assertions.assertEquals(ComparisonOperator.LESS,
                                    valueComparisons.get(1).getOperator(),
                                    "Operator should be GREATER");
            // we expect 12L because deserializer cannot differentiate between Integer and Long
            Assertions.assertEquals(12L, valueComparisons.get(1).getValue(), "Value should be 12");
            Assertions.assertEquals("feature.properties.IntegerA", rangeCriterion.getName());
        } else {
            Assertions.fail("Should be able to deserialize as RangeCriterion");
        }
    }

}
