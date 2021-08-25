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
package fr.cnes.regards.framework.random;

import fr.cnes.regards.framework.random.function.IPropertyGetter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@EnableAutoConfiguration
public class RandomGeneratorIT {

    private static Path BASE = Paths.get("src", "test", "resources");

    @Autowired
    private GeneratorBuilder generatorBuilder;

    @Test
    public void generate() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("template_001.json"));
        randomGenerator.generate(1);
    }

    @Test
    public void integerWithOrWithoutBounds() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("template_002.json"));
        randomGenerator.generate(10);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void badDependencyPath() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("template_003.json"));
        randomGenerator.generate(1);
    }

    @Test
    public void dependencyPath() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("template_004.json"));
        randomGenerator.generate(1);
    }

    @Test
    public void generateGeode() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("2338-template.json"));
        randomGenerator.generate(2);
    }

    @Test
    public void generateUrnFromId() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("idAndUrn.json"));
        randomGenerator.generate(1);
    }

    @Test
    public void deleteByUrn() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("deleteByUrn.json"));
        randomGenerator.generate(1);
    }

    @Test
    public void generateProperty() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("template_005.json"), new PropertyGetter());
        randomGenerator.generate(1);
    }

    @Test
    public void generateUrnFromId2() {
        Generator randomGenerator = generatorBuilder.build(BASE.resolve("idAndUrn2.json"));
        List<Map<String, Object>> results = randomGenerator.generate(1);
        Assert.assertTrue(!results.isEmpty());

        Map<String, Object> generated = results.get(0);
        Assert.assertEquals("URN:FEATURE:DATA:geode:45ba5149-f8e1-3955-8124-1dee76ceb727:V1", generated.get("urn"));
    }

    private class PropertyGetter implements IPropertyGetter {

        @Override
        public String getProperty(String propertyKey) {
            return "value";
        }
    }
}
