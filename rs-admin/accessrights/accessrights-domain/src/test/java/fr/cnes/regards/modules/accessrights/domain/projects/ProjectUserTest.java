/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;

/**
 * Unit testing of {@link ProjectUser}
 *
 * @author Maxime Bouveron
 */
public class ProjectUserTest {

    /**
     * Test ProjectUser
     */
    private ProjectUser projectUser;

    /**
     * Test email
     */
    private final String email = "email";

    /**
     * Test status
     */
    private final UserStatus status = UserStatus.WAITING_ACCOUNT_ACTIVE;

    /**
     * Test metaData
     */
    private final List<MetaData> metaData = new ArrayList<MetaData>();

    /**
     * Test role
     */
    private final Role role = new Role();

    /**
     * Test permissions
     */
    private final List<ResourcesAccess> permissions = new ArrayList<ResourcesAccess>();

    @Before
    public void setUp() {
        metaData.add(new MetaData());
        permissions.add(new ResourcesAccess());
        projectUser = new ProjectUser(email, role, permissions, metaData);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#ProjectUser()}.
     */
    @Test
    public void testProjectUser() {
        final ProjectUser testUser = new ProjectUser();
        Assert.assertEquals(null, testUser.getId());
        Assert.assertEquals(new ArrayList<>(), testUser.getPermissions());
        Assert.assertEquals(new ArrayList<>(), testUser.getMetaData());
        Assert.assertEquals(UserStatus.WAITING_ACCOUNT_ACTIVE, testUser.getStatus());
        Assert.assertEquals(null, testUser.getLastConnection());
        Assert.assertEquals(null, testUser.getLastUpdate());
        Assert.assertEquals(null, testUser.getRole());
        Assert.assertEquals(null, testUser.getEmail());
    }

    /**
     * Test method for
     * {@link ProjectUser#createProjectUser(Long, OffsetDateTime, OffsetDateTime, UserStatus, List, Role, List, String)}.
     */
    @Test
    public void testProjectUserWithParams() {
        final ProjectUser testUser = new ProjectUser(email, role, permissions, metaData);
        Assert.assertEquals(null, testUser.getId());
        Assert.assertEquals(null, testUser.getLastConnection());
        Assert.assertEquals(null, testUser.getLastUpdate());
        Assert.assertEquals(UserStatus.WAITING_ACCOUNT_ACTIVE, testUser.getStatus());
        Assert.assertEquals(email, testUser.getEmail());
        Assert.assertEquals(metaData, testUser.getMetaData());
        Assert.assertEquals(role, testUser.getRole());
        Assert.assertEquals(permissions, testUser.getPermissions());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getId()}.
     */
    @Test
    public void testGetId() {
        final Long id = 0L;
        projectUser.setId(id);
        Assert.assertEquals(id, projectUser.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        final Long newId = 4L;
        projectUser.setId(newId);
        Assert.assertEquals(newId, projectUser.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getLastConnection()}.
     */
    @Test
    public void testGetLastConnection() {
        final OffsetDateTime newValue = OffsetDateTime.now().minusMinutes(5);
        projectUser.setLastConnection(newValue);
        Assert.assertEquals(newValue, projectUser.getLastConnection());
    }

    /**
     * Test method for {@link ProjectUser#setLastConnection(java.time.OffsetDateTime)}.
     */
    @Test
    public void testSetLastConnection() {
        final OffsetDateTime newLastConnection = OffsetDateTime.now();
        projectUser.setLastConnection(newLastConnection);
        Assert.assertEquals(newLastConnection, projectUser.getLastConnection());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getLastUpdate()}.
     */
    @Test
    public void testGetLastUpdate() {
        final OffsetDateTime newValue = OffsetDateTime.now().minusHours(2);
        projectUser.setLastUpdate(newValue);
        Assert.assertEquals(newValue, projectUser.getLastUpdate());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setLastUpdate(java.time.OffsetDateTime)}.
     */
    @Test
    public void testSetLastUpdate() {
        final OffsetDateTime newLastUpdate = OffsetDateTime.now();
        projectUser.setLastUpdate(newLastUpdate);
        Assert.assertEquals(newLastUpdate, projectUser.getLastUpdate());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getStatus()}.
     */
    @Test
    public void testGetStatus() {
        Assert.assertEquals(status, projectUser.getStatus());
    }

    /**
     * Test method for {@link ProjectUser#setStatus(fr.cnes.regards.modules.accessrights.domain.UserStatus)}.
     */
    @Test
    public void testSetStatus() {
        projectUser.setStatus(UserStatus.ACCESS_GRANTED);
        Assert.assertEquals(UserStatus.ACCESS_GRANTED, projectUser.getStatus());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getMetaData()}.
     */
    @Test
    public void testGetMetaData() {
        Assert.assertEquals(metaData, projectUser.getMetaData());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setMetaData(java.util.List)}.
     */
    @Test
    public void testSetMetaData() {
        final List<MetaData> newMetaData = new ArrayList<MetaData>();
        projectUser.setMetaData(newMetaData);
        Assert.assertEquals(newMetaData, projectUser.getMetaData());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getRole()}.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_ADM_ADM_230")
    public void testGetRole() {
        Assert.assertEquals(role, projectUser.getRole());
    }

    /**
     * Test method for {@link ProjectUser#setRole(fr.cnes.regards.modules.accessrights.domain.projects.Role)}.
     */
    @Test
    @Requirement("REGARDS_DSL_DAM_ADM_ADM_230")
    public void testSetRole() {
        final Role newRole = new Role(DefaultRole.PUBLIC.toString(), null);
        projectUser.setRole(newRole);
        Assert.assertEquals(newRole, projectUser.getRole());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getPermissions()}.
     */
    @Test
    public void testGetPermissions() {
        Assert.assertEquals(permissions, projectUser.getPermissions());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setPermissions(java.util.List)}.
     */
    @Test
    public void testSetPermissions() {
        final List<ResourcesAccess> newPermissions = new ArrayList<ResourcesAccess>();
        projectUser.setPermissions(newPermissions);
        Assert.assertEquals(newPermissions, projectUser.getPermissions());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getEmail()}.
     */
    @Test
    public void testGetEmail() {
        Assert.assertEquals(email, projectUser.getEmail());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setEmail(java.lang.String)}.
     */
    @Test
    public void testSetEmail() {
        final String newEmail = "newMail";
        projectUser.setEmail(newEmail);
        Assert.assertEquals(newEmail, projectUser.getEmail());
    }

}
