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
package fr.cnes.regards.modules.project.rest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnectionState;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 * Class TenantControllerIT
 *
 * Tests for REST endpoints to access tenant entities.
 * @author Sébastien Binda
 */
@InstanceTransactional
@ContextConfiguration(classes = { LicenseConfiguration.class })
public class TenantControllerIT extends AbstractRegardsIT {

    private final static Logger LOG = LoggerFactory.getLogger(TenantControllerIT.class);

    /**
     * Instance admin token
     */
    private String instanceAdmintoken;

    @Autowired
    private IProjectRepository projectRepository;

    @Autowired
    private IProjectConnectionRepository projectConnectionRepository;

    private final String TEST_MS = "rs-test-tenant";

    private final String ACTIVE_PROJECT_NAME = "activeProject";

    @Before
    public void initialize() {
        instanceAdmintoken = jwtService.generateToken("test1", getDefaultUserEmail(),
                                                      DefaultRole.INSTANCE_ADMIN.name());

        Project activeProject = new Project("description", "icon", true, ACTIVE_PROJECT_NAME);
        activeProject.setLabel("label");
        Project deletedProject = new Project("description", "icon", true, "deletedProject");
        deletedProject.setDeleted(true);
        deletedProject.setLabel("label");

        ProjectConnection rsTestConnection = new ProjectConnection(activeProject, TEST_MS, "user", "password", "driver",
                "url");
        rsTestConnection.setState(TenantConnectionState.ENABLED);
        ProjectConnection rsTestConnection2 = new ProjectConnection(deletedProject, TEST_MS, "user", "password",
                "driver", "url");
        rsTestConnection2.setState(TenantConnectionState.DISABLED);

        projectRepository.save(activeProject);
        projectRepository.save(deletedProject);

        projectConnectionRepository.save(rsTestConnection);
        projectConnectionRepository.save(rsTestConnection2);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * Check REST Access to project resources and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrievePublicProjectsTest() {
        performGet(TenantController.BASE_PATH + TenantController.MICROSERVICE_PATH, instanceAdmintoken,
                   customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT).expectToHaveSize(JSON_PATH_ROOT, 1)
                           .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT,
                                                                  Matchers.contains(ACTIVE_PROJECT_NAME))),
                   "error", TEST_MS);
    }

}
