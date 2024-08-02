/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.dam.rest.dataaccess;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.dam.dao.dataaccess.IAccessGroupRepository;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessgroup.AccessGroup;
import fr.cnes.regards.modules.dam.rest.DamRestConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * REST module controller
 *
 * @author Marc Sordi
 */
@TestPropertySource(locations = { "classpath:test.properties" },
                    properties = { "spring.jpa.properties.hibernate.default_schema=dam_ag_rest" })
@ContextConfiguration(classes = { DamRestConfiguration.class })
public class AccessGroupControllerIT extends AbstractRegardsIT {

    private static final String ACCESS_GROUPS_ERROR_MSG = "";

    private static final String AG1_NAME = "AG1";

    private static final String AG2_NAME = "AG2";

    @Autowired
    private IAccessGroupRepository accessGroupRepository;

    @Autowired
    private IProjectUsersClient projectUserClientMock;

    @Autowired
    protected IRuntimeTenantResolver runtimetenantResolver;

    @After
    public void clear() {
        runtimetenantResolver.forceTenant(getDefaultTenant());
        accessGroupRepository.deleteAll();
        runtimetenantResolver.clearTenant();
    }

    @Before
    public void init() {
        clear();

        runtimetenantResolver.forceTenant(getDefaultTenant());
        // Replace stubs by mocks
        Mockito.when(projectUserClientMock.retrieveProjectUserByEmail(ArgumentMatchers.any()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(new ProjectUser()), HttpStatus.OK));
        Mockito.when(projectUserClientMock.retrieveProjectUserByEmail(ArgumentMatchers.any()))
               .thenReturn(new ResponseEntity<>(EntityModel.of(new ProjectUser()), HttpStatus.OK));

        AccessGroup ag1 = accessGroupRepository.save(new AccessGroup(AG1_NAME));
        AccessGroup ag2 = new AccessGroup(AG2_NAME);
        ag2.setPublic(true);
        accessGroupRepository.save(ag2);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_810")
    public void testRetrieveAccessGroupsList() {
        performDefaultGet(AccessGroupController.PATH_ACCESS_GROUPS,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT),
                          ACCESS_GROUPS_ERROR_MSG);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_810")
    public void testRetrievePublicAccessGroupsList() {
        performDefaultGet(AccessGroupController.PATH_ACCESS_GROUPS,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT).addParameter("public", "true"),
                          ACCESS_GROUPS_ERROR_MSG);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_810")
    public void testRetrieveAccessGroup() {
        performDefaultGet(AccessGroupController.PATH_ACCESS_GROUPS + AccessGroupController.PATH_ACCESS_GROUPS_NAME,
                          customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT),
                          ACCESS_GROUPS_ERROR_MSG,
                          AG1_NAME);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_810")
    public void testDeleteAccessGroup() {
        performDefaultDelete(AccessGroupController.PATH_ACCESS_GROUPS + AccessGroupController.PATH_ACCESS_GROUPS_NAME,
                             customizer().expectStatusNoContent(),
                             ACCESS_GROUPS_ERROR_MSG,
                             AG2_NAME);
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_810")
    public void testCreateAccessGroup() {
        AccessGroup toBeCreated = new AccessGroup("NameIsNeeded");

        performDefaultPost(AccessGroupController.PATH_ACCESS_GROUPS,
                           toBeCreated,
                           customizer().expectStatusCreated().expectIsNotEmpty(JSON_PATH_ROOT),
                           ACCESS_GROUPS_ERROR_MSG);
    }

}
