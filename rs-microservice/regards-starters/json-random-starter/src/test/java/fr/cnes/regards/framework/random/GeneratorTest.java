/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.random;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class GeneratorTest {

    private Generator generator;

    private static Path BASE = Paths.get("src", "test", "resources");

    @Before
    public void init() {
        generator = new Generator();
    }

    @Test
    public void generate() {
        generator.generate(BASE.resolve("template_001.json"), 1);
    }

    @Test
    public void integerWithOrWithoutBounds() {
        generator.generate(BASE.resolve("template_002.json"), 10);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void badDependencyPath() {
        generator.generate(BASE.resolve("template_003.json"), 1);
    }

    @Test
    public void dependencyPath() {
        generator.generate(BASE.resolve("template_004.json"), 1);
    }

    @Test
    public void generateGeode() {
        generator.generate(BASE.resolve("2338-template.json"), 2);
    }

    @Test
    public void generateUrnFromId() {
        generator.generate(BASE.resolve("idAndUrn.json"), 1);
    }

    @Test
    public void generateUrnFromId2() {
        List<Map<String, Object>> results = generator.generate(BASE.resolve("idAndUrn2.json"), 1);
        Assert.assertTrue(!results.isEmpty());

        Map<String, Object> generated = results.get(0);
        Assert.assertEquals("URN:FEATURE:DATA:geode:45ba5149-f8e1-3955-8124-1dee76ceb727:V1", generated.get("urn"));
    }
}
