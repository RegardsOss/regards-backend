/**
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.projects;

import static org.junit.Assert.fail;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.regards.modules.accessrights.domain.UserStatus;

/**
 * Unit testing of {@link ProjectUser}
 */
public class ProjectUserTest {

    /**
     * Test ProjectUser
     */
    private ProjectUser projectUser;

    /**
     * Test id
     */
    private final Long id = 0L;

    /**
     * Test email
     */
    private final String email = "email";

    /**
     * Test lastConnection
     */
    private final LocalDateTime lastConnection = LocalDateTime.now();

    /**
     * Test lastUpdate
     */
    private final LocalDateTime lastUpdate = LocalDateTime.now();

    /**
     * Test status
     */
    private final UserStatus status = UserStatus.WAITING_ACCESS;

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

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        metaData.add(new MetaData());
        permissions.add(new ResourcesAccess());
        projectUser = new ProjectUser(id, lastConnection, lastUpdate, status, metaData, role, permissions, email);
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#ProjectUser()}.
     */
    @Test
    public void testProjectUser() {
        LocalDateTime now = LocalDateTime.now();
        ProjectUser testUser = new ProjectUser();
        Assert.assertEquals(id, testUser.getId());
        Assert.assertEquals(new ArrayList<>(), testUser.getPermissions());
        Assert.assertEquals(new ArrayList<>(), testUser.getMetaData());
        Assert.assertEquals(UserStatus.WAITING_ACCESS, testUser.getStatus());
        Assert.assertTrue(now.isBefore(testUser.getLastConnection()) || now.isEqual(testUser.getLastConnection()));
        Assert.assertTrue(now.isBefore(testUser.getLastUpdate()) || now.isEqual(testUser.getLastUpdate()));
        Assert.assertEquals(null, testUser.getRole());
        Assert.assertEquals(null, testUser.getEmail());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#ProjectUser(java.lang.Long, java.time.LocalDateTime, java.time.LocalDateTime, fr.cnes.regards.modules.accessrights.domain.UserStatus, java.util.List, fr.cnes.regards.modules.accessrights.domain.projects.Role, java.util.List, java.lang.String)}.
     */
    @Test
    public void testProjectUserWithParams() {
        ProjectUser testUser = new ProjectUser(id, lastConnection, lastUpdate, status, metaData, role, permissions,
                email);
        Assert.assertEquals(id, testUser.getId());
        Assert.assertEquals(lastConnection, testUser.getLastConnection());
        Assert.assertEquals(lastUpdate, testUser.getLastUpdate());
        Assert.assertEquals(status, testUser.getStatus());
        Assert.assertEquals(metaData, testUser.getMetaData());
        Assert.assertEquals(role, testUser.getRole());
        Assert.assertEquals(permissions, testUser.getPermissions());
        Assert.assertEquals(email, testUser.getEmail());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getId()}.
     */
    @Test
    public void testGetId() {
        Assert.assertEquals(id, projectUser.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setId(java.lang.Long)}.
     */
    @Test
    public void testSetId() {
        Long newId = 4L;
        projectUser.setId(newId);
        Assert.assertEquals(newId, projectUser.getId());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getLastConnection()}.
     */
    @Test
    public void testGetLastConnection() {
        Assert.assertEquals(lastConnection, projectUser.getLastConnection());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setLastConnection(java.time.LocalDateTime)}.
     */
    @Test
    public void testSetLastConnection() {
        LocalDateTime newLastConnection = LocalDateTime.now();
        projectUser.setLastConnection(newLastConnection);
        Assert.assertEquals(newLastConnection, projectUser.getLastConnection());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getLastUpdate()}.
     */
    @Test
    public void testGetLastUpdate() {
        Assert.assertEquals(lastUpdate, projectUser.getLastUpdate());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setLastUpdate(java.time.LocalDateTime)}.
     */
    @Test
    public void testSetLastUpdate() {
        LocalDateTime newLastUpdate = LocalDateTime.now();
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
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setStatus(fr.cnes.regards.modules.accessrights.domain.UserStatus)}.
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
        List<MetaData> newMetaData = new ArrayList<MetaData>();
        projectUser.setMetaData(newMetaData);
        Assert.assertEquals(newMetaData, projectUser.getMetaData());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#accept()}.
     */
    @Test
    public void testAccept() {
        projectUser.accept();
        Assert.assertEquals(UserStatus.ACCESS_GRANTED, projectUser.getStatus());

        try {
            projectUser.accept();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {

        }
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#deny()}.
     */
    @Test
    public void testDeny() {
        projectUser.deny();
        Assert.assertEquals(UserStatus.ACCESS_DENIED, projectUser.getStatus());

        try {
            projectUser.deny();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {

        }
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getRole()}.
     */
    @Test
    public void testGetRole() {
        Assert.assertEquals(role, projectUser.getRole());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setRole(fr.cnes.regards.modules.accessrights.domain.projects.Role)}.
     */
    @Test
    public void testSetRole() {
        Role newRole = new Role(4L);
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
        List<ResourcesAccess> newPermissions = new ArrayList<ResourcesAccess>();
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
        String newEmail = "newMail";
        projectUser.setEmail(newEmail);
        Assert.assertEquals(newEmail, projectUser.getEmail());
    }

}
