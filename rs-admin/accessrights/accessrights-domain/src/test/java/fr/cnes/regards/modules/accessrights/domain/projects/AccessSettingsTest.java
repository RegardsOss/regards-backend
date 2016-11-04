/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link AccessSettings}
 *
 * @author Maxime Bouveron
 */
public class AccessSettingsTest {

    /**
     * Test AccessSettings
     */
    private AccessSettings accessSettings;

    /**
     * Test Id
     */
    private final Long id = 0L;

    /**
     * Test mode
     */
    private final String mode = "mode";

    @Before
    public void setUp() {
        accessSettings = new AccessSettings();
        accessSettings.setId(id);
        accessSettings.setMode(mode);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#hashCode()}.
     */
    @Test
    public void testHashCode() {
        final AccessSettings testAccess = new AccessSettings();
        Assert.assertFalse(testAccess.hashCode() == accessSettings.hashCode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#AccessSettings()}.
     */
    @Test
    public void testAccessSettings() {
        final AccessSettings accessTest = new AccessSettings();
        Assert.assertEquals(null, accessTest.getId());
        Assert.assertEquals(null, accessTest.getMode());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, accessSettings.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#getMode()}.
     */
    @Test
    public void testGetMode() {
        Assert.assertEquals(mode, accessSettings.getMode());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        accessSettings.setId(newId);
        Assert.assertEquals(newId, accessSettings.getId());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#setMode(java.lang.String)}.
     */
    @Test
    public void testSetMode() {
        final String newMode = "newMode";
        accessSettings.setMode(newMode);
        Assert.assertEquals(newMode, accessSettings.getMode());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.AccessSettings#equals(java.lang.Object)}.
     */
    @Test
    public void testEqualsObject() {
        final AccessSettings testAccess = new AccessSettings();
        Assert.assertFalse(accessSettings.equals(testAccess));
        Assert.assertTrue(testAccess.equals(testAccess));
    }

}
