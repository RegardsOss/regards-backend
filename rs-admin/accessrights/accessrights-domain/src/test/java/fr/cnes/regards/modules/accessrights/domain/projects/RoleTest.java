/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link Role}
 *
 * @author Maxime Bouveron
 */
public class RoleTest {

    /**
     * Test role
     */
    private Role role;

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test name
     */
    private final String name = "name";

    /**
     * Test parentRole
     */
    private final Role parentRole = new Role();

    /**
     * Test permissions
     */
    private final Set<ResourcesAccess> permissions = new HashSet<>();

    /**
     * Test projectUsers
     */
    private final List<ProjectUser> projectUsers = new ArrayList<>();

    /**
     * Test authorizedAddresses
     */
    private final List<String> authorizedAddresses = new ArrayList<>();

    /**
     * Test isCorsRequestsAuthorized
     */
    private final boolean isCorsRequestsAuthorized = true;

    /**
     * Test corsRequestsAuthorizationEndDate
     */
    private final LocalDateTime corsRequestsAuthorizationEndDate = LocalDateTime.now();

    /**
     * Test isDefault
     */
    private final boolean isDefault = true;

    /**
     * Test isNative
     */
    private final boolean isNative = true;

    @Before
    public void setUp() {
        permissions.add(new ResourcesAccess());
        projectUsers.add(new ProjectUser());
        authorizedAddresses.add("authorizedAddress");
        role = new Role(name, parentRole);
        role.setAuthorizedAddresses(authorizedAddresses);
        role.setCorsRequestsAuthorizationEndDate(corsRequestsAuthorizationEndDate);
        role.setCorsRequestsAuthorized(isCorsRequestsAuthorized);
        role.setDefault(isDefault);
        role.setId(id);
        role.setName(name);
        role.setNative(isNative);
        role.setParentRole(parentRole);
        role.setPermissions(permissions);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#Role()}.
     */
    @Test
    public void testRole() {
        final Role testRole = new Role();
        Assert.assertEquals(null, testRole.getId());
        Assert.assertEquals(null, testRole.getName());
        Assert.assertEquals(false, testRole.isNative());
        Assert.assertEquals(false, testRole.isDefault());
        Assert.assertEquals(null, testRole.getParentRole());
        Assert.assertEquals(new ArrayList<>(), testRole.getPermissions());
        Assert.assertEquals(new ArrayList<>(), testRole.getAuthorizedAddresses());
        Assert.assertEquals(true, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(null, testRole.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for {@link Role#Role(String, Role)}.
     */
    @Test
    public void testRoleWithParams() {
        final Role testRole = new Role(name, parentRole);
        Assert.assertEquals(null, testRole.getId());
        Assert.assertEquals(name, testRole.getName());
        Assert.assertEquals(false, testRole.isNative());
        Assert.assertEquals(false, testRole.isDefault());
        Assert.assertEquals(parentRole, testRole.getParentRole());
        Assert.assertEquals(new ArrayList<>(), testRole.getPermissions());
        Assert.assertEquals(new ArrayList<>(), testRole.getAuthorizedAddresses());
        Assert.assertEquals(true, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(null, testRole.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setNative(boolean)}.
     */
    @Test
    public void testSetNative() {
        role.setNative(!isNative);
        Assert.assertEquals(!isNative, role.isNative());

    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, role.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        role.setId(newId);
        Assert.assertEquals(newId, role.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getName()}.
     */
    @Test
    public void testGetName() {
        Assert.assertEquals(name, role.getName());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getParentRole()}.
     */
    @Test
    public void testGetParentRole() {
        Assert.assertEquals(parentRole, role.getParentRole());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getPermissions()}.
     */
    @Test
    public void testGetPermissions() {
        Assert.assertEquals(permissions, role.getPermissions());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#isDefault()}.
     */
    @Test
    public void testIsDefault() {
        Assert.assertEquals(isDefault, role.isDefault());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#isNative()}.
     */
    @Test
    public void testIsNative() {
        Assert.assertEquals(isNative, role.isNative());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setDefault(boolean)}.
     */
    @Test
    public void testSetDefault() {
        role.setDefault(!isDefault);
        Assert.assertEquals(!isDefault, role.isDefault());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setName(java.lang.String)}.
     */
    @Test
    public void testSetName() {
        final String newName = "newName";
        role.setName(newName);
        Assert.assertEquals(newName, role.getName());
    }

    /**
     * Test method for {@link Role#setParentRole(Role)}.
     */
    @Test
    public void testSetParentRole() {
        final Role newParentRole = new Role("newParentRole", null);
        role.setParentRole(newParentRole);
        Assert.assertEquals(newParentRole, role.getParentRole());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setPermissions(java.util.List)}.
     */
    @Test
    public void testSetPermissions() {
        final Set<ResourcesAccess> newPermissions = new HashSet<ResourcesAccess>();
        final Long localId = 8L;
        newPermissions.add(new ResourcesAccess(localId));
        role.setPermissions(newPermissions);
        Assert.assertEquals(newPermissions, role.getPermissions());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getAuthorizedAddresses()}.
     */
    @Test
    public void testGetAuthorizedAddresses() {
        Assert.assertEquals(authorizedAddresses, role.getAuthorizedAddresses());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setAuthorizedAddresses(java.util.List)}.
     */
    @Test
    public void testSetAuthorizedAddresses() {
        final List<String> newAuthorizedAddresses = new ArrayList<String>();
        newAuthorizedAddresses.add("newAddress");
        role.setAuthorizedAddresses(newAuthorizedAddresses);
        Assert.assertEquals(newAuthorizedAddresses, role.getAuthorizedAddresses());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#isCorsRequestsAuthorized()}.
     */
    @Test
    public void testIsCorsRequestsAuthorized() {
        Assert.assertEquals(isCorsRequestsAuthorized, role.isCorsRequestsAuthorized());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setCorsRequestsAuthorized(boolean)}.
     */
    @Test
    public void testSetCorsRequestsAuthorized() {
        role.setCorsRequestsAuthorized(!isCorsRequestsAuthorized);
        Assert.assertEquals(!isCorsRequestsAuthorized, role.isCorsRequestsAuthorized());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getCorsRequestsAuthorizationEndDate()}.
     */
    @Test
    public void testGetCorsRequestsAuthorizationEndDate() {
        Assert.assertEquals(corsRequestsAuthorizationEndDate, role.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for {@link Role#setCorsRequestsAuthorizationEndDate(LocalDateTime)}.
     */
    @Test
    public void testSetCorsRequestsAuthorizationEndDate() {
        final LocalDateTime newCorsRequestsAuthorizationEndDate = LocalDateTime.now();
        role.setCorsRequestsAuthorizationEndDate(newCorsRequestsAuthorizationEndDate);
        Assert.assertEquals(newCorsRequestsAuthorizationEndDate, role.getCorsRequestsAuthorizationEndDate());
    }

}
