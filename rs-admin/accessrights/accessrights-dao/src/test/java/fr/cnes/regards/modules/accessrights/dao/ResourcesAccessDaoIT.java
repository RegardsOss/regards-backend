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
package fr.cnes.regards.modules.accessrights.dao;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class ResourcesAccessDaoIT
 *
 * Test class for DAO entities ResourcesAccess
 *
 * @author Sébastien Binda

 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@MultitenantTransactional
public class ResourcesAccessDaoIT {

    private static final String MS_NAME = "rs-test";

    private static final String CONTROLLER_NAME1 = "controller1";

    private static final String CONTROLLER_NAME2 = "controller2";

    private static final String USER_URL = "/user";

    private static final String USER_URL2 = "/user2";

    private static final String PUBLIC_URL = "/public";

    private static final String ADMIN_URL = "/admin";

    private Role publicRole;

    private Role userRole;

    private Role adminRole;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IResourcesAccessRepository resourceAccessRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test1");
    }

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     *
     * Initialize repository datas
     *

     */
    @Before
    public void init() {

        /*
         * Create 4 ResourcesAcces
         */
        ResourcesAccess publicResource = new ResourcesAccess("Public resource", MS_NAME, PUBLIC_URL, CONTROLLER_NAME1,
                RequestMethod.GET, DefaultRole.PUBLIC);

        ResourcesAccess userResource = new ResourcesAccess("User resource", MS_NAME, USER_URL, CONTROLLER_NAME1,
                RequestMethod.GET, DefaultRole.REGISTERED_USER);

        ResourcesAccess userResource2 = new ResourcesAccess("Public resource", MS_NAME, USER_URL2, CONTROLLER_NAME2,
                RequestMethod.GET, DefaultRole.PUBLIC);

        ResourcesAccess adminResource = new ResourcesAccess("Admin resource", MS_NAME, ADMIN_URL, CONTROLLER_NAME1,
                RequestMethod.GET, DefaultRole.PROJECT_ADMIN);

        publicResource = resourceAccessRepository.save(publicResource);
        userResource2 = resourceAccessRepository.save(userResource2);
        userResource = resourceAccessRepository.save(userResource);
        adminResource = resourceAccessRepository.save(adminResource);
        /*
         * Create 3 Role
         */
        publicRole = new Role(DefaultRole.PUBLIC.toString(), null);
        publicRole.setNative(true);
        publicRole.addPermission(publicResource);
        publicRole = roleRepository.save(publicRole);

        userRole = new Role(DefaultRole.REGISTERED_USER.toString(), publicRole);
        userRole.setNative(true);

        userRole.addPermission(publicResource);
        userRole.addPermission(userResource2);
        userRole.addPermission(userResource);
        userRole = roleRepository.save(userRole);

        adminRole = new Role(DefaultRole.ADMIN.toString(), userRole);
        adminRole.setNative(true);

        adminRole.addPermission(publicResource);
        adminRole.addPermission(userResource2);
        adminRole.addPermission(userResource);
        adminRole.addPermission(adminResource);
        adminRole = roleRepository.save(adminRole);
    }

    @Test
    public void findByParentRoleName() {
        final Set<Role> roles = roleRepository.findByParentRoleName(DefaultRole.PUBLIC.toString());
        Assert.assertNotNull(roles);
        Assert.assertEquals(1, roles.size());
        Assert.assertNotNull(((Role) roles.toArray()[0]).getPermissions());
        Assert.assertTrue(((Role) roles.toArray()[0]).getPermissions().size() > 0);
    }

    @Test
    public void findManageableResources() {
        List<ResourcesAccess> manageableResources = resourceAccessRepository
                .findManageableResources(MS_NAME, CONTROLLER_NAME1, DefaultRole.PUBLIC.name());
        Assert.assertNotNull(manageableResources);
        Assert.assertEquals(1, manageableResources.size());
        Assert.assertEquals(MS_NAME, manageableResources.get(0).getMicroservice());

        manageableResources = resourceAccessRepository.findManageableResources(MS_NAME, CONTROLLER_NAME1,
                                                                               DefaultRole.REGISTERED_USER.name());
        Assert.assertNotNull(manageableResources);
        Assert.assertEquals(2, manageableResources.size());

        manageableResources = resourceAccessRepository.findManageableResources(MS_NAME, CONTROLLER_NAME1,
                                                                               DefaultRole.ADMIN.name());
        Assert.assertNotNull(manageableResources);
        Assert.assertEquals(3, manageableResources.size());

        manageableResources = resourceAccessRepository.findManageableResources(MS_NAME, CONTROLLER_NAME1, "UNKNOWN");
        Assert.assertNotNull(manageableResources);
        Assert.assertEquals(0, manageableResources.size());
    }

    @Test
    public void findManageableControllers() {
        List<String> manageableControllers = resourceAccessRepository
                .findManageableControllers(MS_NAME, DefaultRole.ADMIN.name());
        Assert.assertNotNull(manageableControllers);
        Assert.assertEquals(2, manageableControllers.size());

        manageableControllers = resourceAccessRepository.findManageableControllers(MS_NAME, DefaultRole.PUBLIC.name());
        Assert.assertNotNull(manageableControllers);
        Assert.assertEquals(1, manageableControllers.size());
    }

}
