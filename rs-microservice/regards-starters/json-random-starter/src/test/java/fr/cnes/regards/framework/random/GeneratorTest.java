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

import org.junit.Test;

public class GeneratorTest {

    private static Generator GENERATOR = new Generator();

    private static Path BASE = Paths.get("src", "test", "resources");

    @Test
    public void generate() {
        GENERATOR.generate(BASE.resolve("template_001.json"), 1);
    }

    @Test
    public void generate2() {
        GENERATOR.generate(BASE.resolve("template_002.json"), 10);
    }

    @Test
    public void generateGeode() {
        GENERATOR.generate(BASE.resolve("2338-template.json"), 2);
    }
}
