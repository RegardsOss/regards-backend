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

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * @author Marc Sordi
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class UserResourceControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @MockBean
    private QuotaHelperService quotaHelperService;

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to retrieve a user's permissions.")
    public void getUserPermissions() {

        // Create user
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        ProjectUser user = projectUserRepository.save(new ProjectUser(getDefaultUserEmail(),
                                                                      adminRole,
                                                                      new ArrayList<>(),
                                                                      new HashSet<>()));

        // Create a new resource
        ResourcesAccess resource = new ResourcesAccess(null,
                                                       "microservice",
                                                       "/to/user",
                                                       "controller",
                                                       RequestMethod.GET,
                                                       DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);

        // Add access to user
        user.setPermissions(Lists.newArrayList(resource));

        performDefaultGet(UserResourceController.TYPE_MAPPING,
                          customizer().expectStatusOk(),
                          "Error retrieving resourcesAccess for user.",
                          user.getEmail());
    }

    @Test
    public void testGetPermissionsUserUnknown() {

        // Create another user
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        ProjectUser user = projectUserRepository.save(new ProjectUser(getDefaultUserEmail(),
                                                                      adminRole,
                                                                      new ArrayList<>(),
                                                                      new HashSet<>()));

        // Create a new resource
        ResourcesAccess resource = new ResourcesAccess(null,
                                                       "microservice",
                                                       "/to/user",
                                                       "controller",
                                                       RequestMethod.GET,
                                                       DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);
        // Add access to the other user
        user.setPermissions(Lists.newArrayList(resource));

        performDefaultGet(UserResourceController.TYPE_MAPPING,
                          customizer().expectStatusNotFound(),
                          "The user does not exists. There should be an error 404",
                          "wrongEmail");
    }

}
