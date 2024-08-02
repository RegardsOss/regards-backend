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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        Assert.assertNull(testRole.getId());
        Assert.assertNull(testRole.getName());
        Assert.assertFalse(testRole.isNative());
        Assert.assertFalse(testRole.isDefault());
        Assert.assertNull(testRole.getParentRole());
        Assert.assertEquals(new HashSet<>(), testRole.getPermissions());
    }

    /**
     * Test method for {@link Role#Role(String, Role)}.
     */
    @Test
    public void testRoleWithParams() {
        final Role testRole = new Role(name, parentRole);
        Assert.assertNull(testRole.getId());
        Assert.assertEquals(name, testRole.getName());
        Assert.assertFalse(testRole.isNative());
        Assert.assertFalse(testRole.isDefault());
        Assert.assertEquals(parentRole, testRole.getParentRole());
        Assert.assertEquals(new HashSet<>(), testRole.getPermissions());
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

    @Test
    public void testSetPermissions() {
        final Set<ResourcesAccess> newPermissions = new HashSet<>();
        final Long localId = 8L;
        newPermissions.add(new ResourcesAccess(localId));
        role.setPermissions(newPermissions);
        Assert.assertEquals(newPermissions, role.getPermissions());
    }

}
