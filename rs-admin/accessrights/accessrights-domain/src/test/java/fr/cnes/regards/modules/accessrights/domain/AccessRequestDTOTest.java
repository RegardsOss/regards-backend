/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.passwordreset.PerformResetPasswordDto;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 * Unit test for {@link PerformResetPasswordDto}
 *
 * @author Maxime Bouveron
 */
public class AccessRequestDTOTest {

    /**
     * Test PerformResetPasswordDto
     */
    private AccessRequestDto access;

    /**
     * Test email
     */
    private final String email = "email";

    /**
     * Test first name
     */
    private final String firstName = "firstName";

    /**
     * Test last name
     */
    private final String lastName = "lastName";

    /**
     * Test MetaData
     */
    private final List<MetaData> metaDatas = new ArrayList<>();

    /**
     * Test password
     */
    private final String password = "password";

    /**
     * The origin url
     */
    private String originUrl;

    /**
     * The request link
     */
    private String requestLink;

    /**
     * Setup
     */
    @Before
    public void setUp() {
        access = new AccessRequestDto(email, firstName, lastName, metaDatas, password, originUrl, requestLink);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, access.getEmail());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getFirstName()}.
     */
    @Test
    public void testGetFirstName() {
        Assert.assertEquals(firstName, access.getFirstName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getLastName()}.
     */
    @Test
    public void testGetLastName() {
        Assert.assertEquals(lastName, access.getLastName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getMetaData()}.
     */
    @Test
    public void testGetMetaData() {
        Assert.assertEquals(metaDatas, access.getMetaData());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getPassword()}.
     */
    @Test
    public void testGetPassword() {
        Assert.assertEquals(password, access.getPassword());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setEmail(java.lang.String)}.
     */
    @Test
    public void testSetEmail() {
        final String newEmail = "newEmail";
        access.setEmail(newEmail);
        Assert.assertEquals(newEmail, access.getEmail());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setFirstName(java.lang.String)}.
     */
    @Test
    public void testSetFirstName() {
        final String newFirstName = "newFirstName";
        access.setFirstName(newFirstName);
        Assert.assertEquals(newFirstName, access.getFirstName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setLastName(java.lang.String)}.
     */
    @Test
    public void testSetLastName() {
        final String newLastName = "newLastName";
        access.setLastName(newLastName);
        Assert.assertEquals(newLastName, access.getLastName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setMetaData(java.util.List)}.
     */
    @Test
    public void testSetMetaData() {
        final List<MetaData> newMetaData = new ArrayList<MetaData>();
        newMetaData.add(new MetaData());
        access.setMetaData(newMetaData);
        Assert.assertEquals(newMetaData, access.getMetaData());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setPassword(java.lang.String)}.
     */
    @Test
    public void testSetPassword() {
        final String newPassword = "newPassword";
        access.setPassword(newPassword);
        Assert.assertEquals(newPassword, access.getPassword());
    }

}
