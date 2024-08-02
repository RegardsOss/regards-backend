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

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.domain.UserStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final Set<MetaData> metaData = new HashSet<>();

    /**
     * Test role
     */
    private final Role role = new Role();

    /**
     * Test permissions
     */
    private final List<ResourcesAccess> permissions = new ArrayList<>();

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
        Assert.assertNull(testUser.getId());
        Assert.assertEquals(new ArrayList<>(), testUser.getPermissions());
        Assert.assertEquals(new HashSet<>(), testUser.getMetadata());
        Assert.assertEquals(UserStatus.WAITING_ACCOUNT_ACTIVE, testUser.getStatus());
        Assert.assertNull(testUser.getLastConnection());
        Assert.assertNull(testUser.getLastUpdate());
        Assert.assertNull(testUser.getRole());
        Assert.assertNull(testUser.getEmail());
    }

    @Test
    public void testProjectUserWithParams() {
        final ProjectUser testUser = new ProjectUser(email, role, permissions, metaData);
        Assert.assertNull(testUser.getId());
        Assert.assertNull(testUser.getLastConnection());
        Assert.assertNull(testUser.getLastUpdate());
        Assert.assertEquals(UserStatus.WAITING_ACCOUNT_ACTIVE, testUser.getStatus());
        Assert.assertEquals(email, testUser.getEmail());
        Assert.assertEquals(metaData, testUser.getMetadata());
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
     * Test method for {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#getMetadata()}.
     */
    @Test
    public void testGetMetaData() {
        Assert.assertEquals(metaData, projectUser.getMetadata());
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser#setMetadata(java.util.Set)}.
     */
    @Test
    public void testSetMetaData() {
        final Set<MetaData> newMetaData = new HashSet<>();
        projectUser.setMetadata(newMetaData);
        Assert.assertEquals(newMetaData, projectUser.getMetadata());
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
        final List<ResourcesAccess> newPermissions = new ArrayList<>();
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
