/**
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 * <p>
 * This file is part of REGARDS.
 * <p>
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

/**
 * Unit testing of {@link RoleAuthorizedAdressesConverter}
 *
 * @author Maxime Bouveron
 */
public class RoleAuthorizedAdressesConverterTest {

    /**
     * Test RoleAuthorizedAdressesConverter
     */
    private final RoleAuthorizedAdressesConverter converter = new RoleAuthorizedAdressesConverter();

    /**
     * Strings used to test the converter
     */
    private final ArrayList<String> strings = new ArrayList<>();

    /**
     * String used to test the converter
     */
    private final String string = "value1;value2 ; value3;value 4;value5;value6";

    @Before
    public void setUp() {
        strings.add("value1");
        strings.add("value2 ");
        strings.add(" value3");
        strings.add("value 4");
        strings.add("value5");
        strings.add("value6");
    }

    /**
     * Test method for {@link RoleAuthorizedAdressesConverter#convertToDatabaseColumn(java.util.List)}.
     */
    @Test
    public void testConvertToDatabaseColumn() {
        Assert.assertNull(converter.convertToDatabaseColumn(null));
        Assert.assertNull(converter.convertToDatabaseColumn(new ArrayList<>()));
        Assert.assertEquals(string, converter.convertToDatabaseColumn(strings));
    }

    /**
     * Test method for {@link RoleAuthorizedAdressesConverter#convertToEntityAttribute(java.lang.String)}.
     */
    @Test
    public void testConvertToEntityAttribute() {
        Assert.assertEquals(strings, converter.convertToEntityAttribute(string));
    }

}
