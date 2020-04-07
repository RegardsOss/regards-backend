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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.random.function.FunctionDescriptorParser;

public class FunctionParserTests {

    private static final Logger LOGGER = LoggerFactory.getLogger(FunctionParserTests.class);

    @Test
    public void testInteger() {
        FunctionDescriptorParser.parse("");
        FunctionDescriptorParser.parse("{{integer(3,12)}}");
        FunctionDescriptorParser.parse("{{float()}}");
        FunctionDescriptorParser.parse("{{float(0,'2,0')}}");
    }
}
