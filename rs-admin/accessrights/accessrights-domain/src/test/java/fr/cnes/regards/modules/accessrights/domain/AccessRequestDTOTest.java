/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;

/**
 * Unit test for {@link AccessRequestDto}
 *
 * @author Maxime Bouveron
 */
public class AccessRequestDTOTest {

    /**
     * Test AccessRequestDto
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
    private List<MetaData> metaDatas;

    /**
     * Test password
     */
    private final String password = "password";

    /**
     * Test permissions
     */
    private List<ResourcesAccess> permissions;

    /**
     * Test role
     */
    private Role role;

    /**
     * Setup
     */
    @Before
    public void setUp() {
        access = new AccessRequestDto();
        metaDatas = new ArrayList<MetaData>();
        metaDatas.add(new MetaData());

        permissions = new ArrayList<ResourcesAccess>();
        permissions.add(new ResourcesAccess());

        access.setEmail(email);
        access.setFirstName(firstName);
        access.setLastName(lastName);
        access.setPassword(password);
        access.setMetaData(metaDatas);
        access.setPermissions(permissions);

        role = new Role(DefaultRole.ADMIN.toString(), null);
        access.setRoleName(role.getName());
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
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getPermissions()}.
     */
    @Test
    public void testGetPermissions() {
        Assert.assertEquals(permissions, access.getPermissions());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#getRole()}.
     */
    @Test
    public void testGetRoleName() {
        Assert.assertEquals(role.getName(), access.getRoleName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setEmail(java.lang.String)}.
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
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setMetaData(java.util.List)}.
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

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto#setPermissions(java.util.List)}.
     */
    @Test
    public void testSetPermissions() {
        final List<ResourcesAccess> newPermissions = new ArrayList<ResourcesAccess>();
        newPermissions.add(new ResourcesAccess());
        access.setPermissions(newPermissions);
        Assert.assertEquals(newPermissions, access.getPermissions());
    }

    /**
     * Test method for {@link AccessRequestDto#setRole(Role)}.
     */
    @Test
    public void testSetRoleName() {
        final String newRoleName = DefaultRole.REGISTERED_USER.toString();
        access.setRoleName(newRoleName);
        Assert.assertEquals(newRoleName, access.getRoleName());
    }

}
