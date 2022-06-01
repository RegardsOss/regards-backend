/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.rest;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * @author Marc Sordi
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class RoleResourceControllerIT extends AbstractRegardsTransactionalIT {

    private static final String NEW_ROLE_NAME = "NEW_ROLE";

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private IRoleService roleService;

    @MockBean
    private QuotaHelperService quotaHelperService;

    /**
     * Get all role resources
     */
    @Test
    public void getRoleResourcesTest() {
    }

    /**
     * Add new resource access to a role
     */
    @Test
    public void addRoleResourceTest() {
    }

    /**
     * Removing {@link DefaultRole#ADMIN} access from role inheriting ADMIN changes its parent to
     * {@link DefaultRole#EXPLOIT}
     *
     * @throws EntityException if error occurs
     */
    @Test
    public void adminToRuParentTest() throws EntityException {
        changeParentTest(DefaultRole.ADMIN, HttpStatus.NO_CONTENT, DefaultRole.EXPLOIT);
    }

    /**
     * Removing {@link DefaultRole#REGISTERED_USER} access from role inheriting ADMIN changes its parent to
     * {@link DefaultRole#PUBLIC}
     *
     * @throws EntityException if error occurs
     */
    @Test
    @Ignore("No default resource with REGISTERED USER at the moment")
    public void adminToPublicParentTest() throws EntityException {
        changeParentTest(DefaultRole.REGISTERED_USER, HttpStatus.NO_CONTENT, DefaultRole.PUBLIC);
    }

    /**
     * Removing {@link DefaultRole} access from role inheriting ADMIN changes its parent to {@link DefaultRole#PUBLIC}
     *
     * @throws EntityException if error occurs
     */
    @Test
    public void adminToNoRoleParentTest() throws EntityException {
        changeParentTest(DefaultRole.PUBLIC, HttpStatus.FORBIDDEN, null);
    }

    /**
     * Automatically change native role parent when removing resource access
     *
     * @param removedAccessDefaultRole access to remove
     * @param expectedStatus           expected HTTP status
     * @param expected                 expected native parent role
     * @throws EntityException if error occurs
     */
    private void changeParentTest(DefaultRole removedAccessDefaultRole, HttpStatus expectedStatus, DefaultRole expected)
        throws EntityException {

        // Retrieve default admin role
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();

        // Create role inheriting from admin
        Role newRole = roleService.createRole(new Role(NEW_ROLE_NAME, adminRole));

        Assert.assertNotNull(newRole.getPermissions());

        // Remove first admin inherited permission from new role
        ResourcesAccess resourceToRemove = null;
        for (ResourcesAccess access : newRole.getPermissions()) {
            if (removedAccessDefaultRole.equals(access.getDefaultRole())) {
                resourceToRemove = access;
                break;
            }
        }

        // Check resource not null
        Assert.assertNotNull("Cannot find an ADMIN resources", resourceToRemove);

        // Delete resource with PROJECT ADMIN
        String projectAdminJwt = manageSecurity(getDefaultTenant(),
                                                RoleResourceController.TYPE_MAPPING
                                                + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                                                RequestMethod.DELETE,
                                                getDefaultUserEmail(),
                                                DefaultRole.PROJECT_ADMIN.name());
        performDelete(RoleResourceController.TYPE_MAPPING + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                      projectAdminJwt,
                      customizer().expect(MockMvcResultMatchers.status().is(expectedStatus.value())),
                      "Error retrieving resourcesAccess for user.",
                      newRole.getName(),
                      resourceToRemove.getId());

        // Check new role associated to expected one
        if (expected != null) {
            Assert.assertEquals(expected.name(), newRole.getParentRole().getName());
        }
    }

    /**
     * Remove resource to {@link DefaultRole#ADMIN} change inherited role parent to {@link DefaultRole#EXPLOIT}
     *
     * @throws EntityException if error occurs
     */
    @Test
    public void addAndRestoreResource() throws EntityException {
        // Retrieve default admin role
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();

        // Create role inheriting from admin
        Role newRole = roleService.createRole(new Role(NEW_ROLE_NAME, adminRole));

        Assert.assertNotNull(newRole.getPermissions());
        Assert.assertFalse("newRole has no permissions", newRole.getPermissions().isEmpty());

        // Remove first admin inherited permission from new role
        ResourcesAccess resourceToRemove = null;
        for (ResourcesAccess access : newRole.getPermissions()) {
            if (DefaultRole.ADMIN.equals(access.getDefaultRole())) {
                resourceToRemove = access;
                break;
            }
        }

        // Check resource not null
        Assert.assertNotNull("Cannot find an ADMIN resources", resourceToRemove);

        // Delete resource with PROJECT ADMIN
        String projectAdminJwt = manageSecurity(getDefaultTenant(),
                                                RoleResourceController.TYPE_MAPPING
                                                + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                                                RequestMethod.DELETE,
                                                getDefaultUserEmail(),
                                                DefaultRole.PROJECT_ADMIN.name());
        performDelete(RoleResourceController.TYPE_MAPPING + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                      projectAdminJwt,
                      customizer().expectStatusNoContent(),
                      "Error retrieving resourcesAccess for user.",
                      newRole.getName(),
                      resourceToRemove.getId());

        // Check new role associated to REGISTERED USER instead of ADMIN
        Assert.assertEquals(DefaultRole.EXPLOIT.name(), newRole.getParentRole().getName());

        // Add resource with ADMIN
        String adminJwt = manageSecurity(getDefaultTenant(),
                                         RoleResourceController.TYPE_MAPPING,
                                         RequestMethod.POST,
                                         getDefaultUserEmail(),
                                         DefaultRole.ADMIN.name());
        performPost(RoleResourceController.TYPE_MAPPING,
                    adminJwt,
                    resourceToRemove,
                    customizer().expectStatusCreated(),
                    "Access should be added to ADMIN role",
                    newRole.getName());

        // Check new role associated to REGISTERED USER instead of ADMIN
        Assert.assertEquals(DefaultRole.ADMIN.name(), newRole.getParentRole().getName());
    }

    /**
     * Delete resource access from a role
     */
    @Test
    @Purpose("Check that the system allows to remove a resourceAccess permission to a given role.")
    public void deleteRoleResourceTest() {

        // Create a new resource
        ResourcesAccess resource = new ResourcesAccess(null,
                                                       "microservice",
                                                       "/to/delete",
                                                       "delController",
                                                       RequestMethod.GET,
                                                       DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);

        // Add to admin group
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        adminRole.addPermission(resource);
        roleRepository.save(adminRole);

        // Delete resource with PROJECT ADMIN
        String projectAdminJwt = manageSecurity(getDefaultTenant(),
                                                RoleResourceController.TYPE_MAPPING
                                                + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                                                RequestMethod.DELETE,
                                                getDefaultUserEmail(),
                                                DefaultRole.PROJECT_ADMIN.name());
        performDelete(RoleResourceController.TYPE_MAPPING + RoleResourceController.SINGLE_RESOURCE_MAPPING,
                      projectAdminJwt,
                      customizer().expectStatusNoContent(),
                      "Error retrieving resourcesAccess for user.",
                      adminRole.getName(),
                      resource.getId());
    }

}
