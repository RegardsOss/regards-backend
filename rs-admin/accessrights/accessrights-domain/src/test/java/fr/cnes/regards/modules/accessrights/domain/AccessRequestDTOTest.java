/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * Unit test for {@link AccessRequestDTO}
 *
 * @author Maxime Bouveron
 */
public class AccessRequestDTOTest {

    /**
     * Test AccessRequestDTO
     */
    private AccessRequestDTO access;

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
     * Test login
     */
    private final String login = "login";

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
        access = new AccessRequestDTO();
        metaDatas = new ArrayList<MetaData>();
        metaDatas.add(new MetaData());

        permissions = new ArrayList<ResourcesAccess>();
        permissions.add(new ResourcesAccess());

        access.setEmail(email);
        access.setFirstName(firstName);
        access.setLastName(lastName);
        access.setLogin(login);
        access.setPassword(password);
        access.setMetaData(metaDatas);
        access.setPermissions(permissions);

        role = new Role(0L);
        access.setRole(role);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, access.getEmail());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getFirstName()}.
     */
    @Test
    public void testGetFirstName() {
        Assert.assertEquals(firstName, access.getFirstName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getLastName()}.
     */
    @Test
    public void testGetLastName() {
        Assert.assertEquals(lastName, access.getLastName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getLogin()}.
     */
    @Test
    public void testGetLogin() {
        Assert.assertEquals(login, access.getLogin());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getMetaData()}.
     */
    @Test
    public void testGetMetaData() {
        Assert.assertEquals(metaDatas, access.getMetaData());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getPassword()}.
     */
    @Test
    public void testGetPassword() {
        Assert.assertEquals(password, access.getPassword());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getPermissions()}.
     */
    @Test
    public void testGetPermissions() {
        Assert.assertEquals(permissions, access.getPermissions());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#getRole()}.
     */
    @Test
    public void testGetRole() {
        Assert.assertEquals(role, access.getRole());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setEmail(java.lang.String)}.
     */
    @Test
    public void testSetEmail() {
        final String newEmail = "newEmail";
        access.setEmail(newEmail);
        Assert.assertEquals(newEmail, access.getEmail());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setFirstName(java.lang.String)}.
     */
    @Test
    public void testSetFirstName() {
        final String newFirstName = "newFirstName";
        access.setFirstName(newFirstName);
        Assert.assertEquals(newFirstName, access.getFirstName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setLastName(java.lang.String)}.
     */
    @Test
    public void testSetLastName() {
        final String newLastName = "newLastName";
        access.setLastName(newLastName);
        Assert.assertEquals(newLastName, access.getLastName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setLogin(java.lang.String)}.
     */
    @Test
    public void testSetLogin() {
        final String newLogin = "newLogin";
        access.setLogin(newLogin);
        Assert.assertEquals(newLogin, access.getLogin());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setMetaData(java.util.List)}.
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
     * {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setPassword(java.lang.String)}.
     */
    @Test
    public void testSetPassword() {
        final String newPassword = "newPassword";
        access.setPassword(newPassword);
        Assert.assertEquals(newPassword, access.getPassword());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.AccessRequestDTO#setPermissions(java.util.List)}.
     */
    @Test
    public void testSetPermissions() {
        final List<ResourcesAccess> newPermissions = new ArrayList<ResourcesAccess>();
        newPermissions.add(new ResourcesAccess());
        access.setPermissions(newPermissions);
        Assert.assertEquals(newPermissions, access.getPermissions());
    }

    /**
     * Test method for {@link AccessRequestDTO#setRole(Role)}.
     */
    @Test
    public void testSetRole() {
        final Role newRole = new Role(1L);
        access.setRole(newRole);
        Assert.assertEquals(newRole, access.getRole());
    }

}
