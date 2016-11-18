/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
    private final ArrayList<String> strings = new ArrayList<String>();

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
        Assert.assertEquals(null, converter.convertToDatabaseColumn(null));
        Assert.assertEquals(null, converter.convertToDatabaseColumn(new ArrayList<>()));
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
