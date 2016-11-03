/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit testing of {@link Role}
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
    private final List<ResourcesAccess> permissions = new ArrayList<ResourcesAccess>();

    /**
     * Test projectUsers
     */
    private final List<ProjectUser> projectUsers = new ArrayList<ProjectUser>();

    /**
     * Test authorizedAddresses
     */
    private final List<String> authorizedAddresses = new ArrayList<String>();

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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        permissions.add(new ResourcesAccess());
        projectUsers.add(new ProjectUser());
        authorizedAddresses.add("authorizedAddress");

        role = new Role(id, name, parentRole, permissions, authorizedAddresses, projectUsers, isDefault, isNative,
                isCorsRequestsAuthorized, corsRequestsAuthorizationEndDate);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#Role()}.
     */
    @Test
    public void testRole() {
        Role testRole = new Role();
        Assert.assertEquals(null, testRole.getId());
        Assert.assertEquals(null, testRole.getName());
        Assert.assertEquals(false, testRole.isNative());
        Assert.assertEquals(false, testRole.isDefault());
        Assert.assertEquals(null, testRole.getParentRole());
        Assert.assertEquals(null, testRole.getPermissions());
        Assert.assertEquals(null, testRole.getProjectUsers());
        Assert.assertEquals(null, testRole.getAuthorizedAddresses());
        Assert.assertEquals(false, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(null, testRole.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#Role(java.lang.Long)}.
     */
    @Test
    public void testRoleWithId() {
        Role testRole = new Role(id);
        Assert.assertEquals(id, testRole.getId());
        Assert.assertEquals(null, testRole.getName());
        Assert.assertEquals(false, testRole.isNative());
        Assert.assertEquals(false, testRole.isDefault());
        Assert.assertEquals(null, testRole.getParentRole());
        Assert.assertEquals(null, testRole.getPermissions());
        Assert.assertEquals(null, testRole.getProjectUsers());
        Assert.assertEquals(null, testRole.getAuthorizedAddresses());
        Assert.assertEquals(false, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(null, testRole.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#Role(java.lang.Long, java.lang.String, fr.cnes.regards.modules.accessrights.domain.projects.Role, java.util.List, java.util.List)}.
     */
    @Test
    public void testRoleWithParams() {
        Role testRole = new Role(id, name, parentRole, permissions, projectUsers);
        Assert.assertEquals(id, testRole.getId());
        Assert.assertEquals(name, testRole.getName());
        Assert.assertEquals(false, testRole.isNative());
        Assert.assertEquals(false, testRole.isDefault());
        Assert.assertEquals(parentRole, testRole.getParentRole());
        Assert.assertEquals(permissions, testRole.getPermissions());
        Assert.assertEquals(projectUsers, testRole.getProjectUsers());
        Assert.assertEquals(null, testRole.getAuthorizedAddresses());
        Assert.assertEquals(false, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(null, testRole.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#Role(java.lang.Long, java.lang.String, fr.cnes.regards.modules.accessrights.domain.projects.Role, java.util.List, java.util.List, boolean, boolean)}.
     */
    @Test
    public void testRoleWithoutCORS() {
        Role testRole = new Role(id, name, parentRole, permissions, projectUsers, isDefault, isNative);
        Assert.assertEquals(id, testRole.getId());
        Assert.assertEquals(name, testRole.getName());
        Assert.assertEquals(isNative, testRole.isNative());
        Assert.assertEquals(isDefault, testRole.isDefault());
        Assert.assertEquals(parentRole, testRole.getParentRole());
        Assert.assertEquals(permissions, testRole.getPermissions());
        Assert.assertEquals(projectUsers, testRole.getProjectUsers());
        Assert.assertEquals(null, testRole.getAuthorizedAddresses());
        Assert.assertEquals(false, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(null, testRole.getCorsRequestsAuthorizationEndDate());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#Role(java.lang.Long, java.lang.String, fr.cnes.regards.modules.accessrights.domain.projects.Role, java.util.List, java.util.List, java.util.List, boolean, boolean, boolean, java.time.LocalDateTime)}.
     */
    @Test
    public void testRoleFull() {
        Role testRole = new Role(id, name, parentRole, permissions, authorizedAddresses, projectUsers, isDefault,
                isNative, isCorsRequestsAuthorized, corsRequestsAuthorizationEndDate);
        Assert.assertEquals(id, testRole.getId());
        Assert.assertEquals(name, testRole.getName());
        Assert.assertEquals(isNative, testRole.isNative());
        Assert.assertEquals(isDefault, testRole.isDefault());
        Assert.assertEquals(parentRole, testRole.getParentRole());
        Assert.assertEquals(permissions, testRole.getPermissions());
        Assert.assertEquals(projectUsers, testRole.getProjectUsers());
        Assert.assertEquals(authorizedAddresses, testRole.getAuthorizedAddresses());
        Assert.assertEquals(isCorsRequestsAuthorized, testRole.isCorsRequestsAuthorized());
        Assert.assertEquals(corsRequestsAuthorizationEndDate, testRole.getCorsRequestsAuthorizationEndDate());
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
        Long newId = 4L;
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
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#getProjectUsers()}.
     */
    @Test
    public void testGetProjectUsers() {
        Assert.assertEquals(projectUsers, role.getProjectUsers());
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
        String newName = "newName";
        role.setName(newName);
        Assert.assertEquals(newName, role.getName());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setParentRole(fr.cnes.regards.modules.accessrights.domain.projects.Role)}.
     */
    @Test
    public void testSetParentRole() {
        Role newParentRole = new Role(8L);
        role.setParentRole(newParentRole);
        Assert.assertEquals(newParentRole, role.getParentRole());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setPermissions(java.util.List)}.
     */
    @Test
    public void testSetPermissions() {
        List<ResourcesAccess> newPermissions = new ArrayList<ResourcesAccess>();
        newPermissions.add(new ResourcesAccess(8L));
        role.setPermissions(newPermissions);
        Assert.assertEquals(newPermissions, role.getPermissions());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setProjectUsers(java.util.List)}.
     */
    @Test
    public void testSetProjectUsers() {
        List<ProjectUser> newProjectUsers = new ArrayList<ProjectUser>();
        newProjectUsers.add(new ProjectUser());
        role.setProjectUsers(newProjectUsers);
        Assert.assertEquals(newProjectUsers, role.getProjectUsers());
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
        List<String> newAuthorizedAddresses = new ArrayList<String>();
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
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.Role#setCorsRequestsAuthorizationEndDate(java.time.LocalDateTime)}.
     */
    @Test
    public void testSetCorsRequestsAuthorizationEndDate() {
        LocalDateTime newCorsRequestsAuthorizationEndDate = LocalDateTime.now();
        role.setCorsRequestsAuthorizationEndDate(newCorsRequestsAuthorizationEndDate);
        Assert.assertEquals(newCorsRequestsAuthorizationEndDate, role.getCorsRequestsAuthorizationEndDate());
    }

}
