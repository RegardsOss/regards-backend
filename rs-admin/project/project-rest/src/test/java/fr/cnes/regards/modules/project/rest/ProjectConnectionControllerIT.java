/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 * Class ProjectConnectionControllerIT
 *
 * Tests REST endpoint to access ProjectConnection entities
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
@InstanceTransactional
@ContextConfiguration(classes = { LicenseConfiguration.class })
public class ProjectConnectionControllerIT extends AbstractRegardsIT {

    /**
     * Class logger
     */
    private final static Logger LOG = LoggerFactory.getLogger(ProjectConnectionControllerIT.class);

    /**
     * Name of the project used for tests
     */
    private final static String PROJECT_TEST = "test1";

    /**
     * Name of the microservice used for tests
     */
    private final static String MICROSERVICE_TEST = "microservice-test";

    /**
     * Token for instance admin user
     */
    private String instanceAdmintoken;

    /**
     * JPA Repository for direct access to Project entities
     */
    @Autowired
    private IProjectRepository projectRepo;

    /**
     * JPA Repository for direct access to ProjectConnection entities
     */
    @Autowired
    private IProjectConnectionRepository projectConnRepo;

    /**
     * A project connection
     */
    private ProjectConnection connection;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * Initialize token and datas
     */
    @Before
    public void initialize() {
        instanceAdmintoken = jwtService.generateToken(PROJECT_TEST, "public@regards.fr",
                                                      DefaultRole.INSTANCE_ADMIN.name());

        Project project = projectRepo.findOneByNameIgnoreCase(PROJECT_TEST);
        project.setLabel("project");
        project = projectRepo.save(project);
        connection = new ProjectConnection(project, MICROSERVICE_TEST, "newUserName", "newPassword", "newDriver",
                "newUrl");
        projectConnRepo.save(connection);
    }

    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to all projects database connections and Hateoas returned links")
    @Test
    public void getAllProjectConnectionsTest() {
        performGet(ProjectConnectionController.TYPE_MAPPING, instanceAdmintoken,
                   customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT)
                           .expectValue(JSON_PATH_ROOT + ".metadata.size", 20)
                           .expectValue(JSON_PATH_ROOT + ".metadata.totalElements", 1)
                           .expectValue(JSON_PATH_ROOT + ".metadata.totalPages", 1)
                           .expectValue(JSON_PATH_ROOT + ".metadata.number", 0).addParameter("size", "20"),
                   "error", PROJECT_TEST);
    }

    /**
     * Check REST Access to get a project connection and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to get a project connection and Hateoas returned links")
    @Test
    public void getProjectConnectionTest() {
        performGet(ProjectConnectionController.TYPE_MAPPING + ProjectConnectionController.RESOURCE_ID_MAPPING,
                   instanceAdmintoken, customizer().expectStatusOk(), "error", PROJECT_TEST, connection.getId());
    }

    /**
     * Check REST Access to create a project connection and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to create a project connection and Hateoas returned links")
    @Test
    public void createProjectConnectionTest() {
        Project project = projectRepo.findOneByNameIgnoreCase(PROJECT_TEST);
        ProjectConnection connection = new ProjectConnection(project, "microservice-test-2", "newUserName",
                "newPassword", "newDriver", "newUrl");
        performPost(ProjectConnectionController.TYPE_MAPPING, instanceAdmintoken, connection,
                    customizer().expectStatusOk(), "Error there must be project results", PROJECT_TEST);
    }

    /**
     * Check REST Access to update a project connection and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to update a project connection and Hateoas returned links")
    @Test
    public void updateProjectConnectionTest() {
        ProjectConnection connection = projectConnRepo.findOneByProjectNameAndMicroservice(PROJECT_TEST,
                                                                                           MICROSERVICE_TEST);
        performPut(ProjectConnectionController.TYPE_MAPPING + ProjectConnectionController.RESOURCE_ID_MAPPING,
                   instanceAdmintoken, connection, customizer().expectStatusOk(), "Error there must be project results",
                   PROJECT_TEST, connection.getId());
    }

    /**
     * Check REST Access to project connections by id. >>>>>>> 538fc5b3af67db38dc598432cc06e9e4134c9971
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to update a project connection and Hateoas returned links")
    @Test
    public void deleteProjectConnectionTest() {
        performDelete(ProjectConnectionController.TYPE_MAPPING + ProjectConnectionController.RESOURCE_ID_MAPPING,
                      instanceAdmintoken, customizer().expectStatusNoContent(), "Error there must be project results",
                      PROJECT_TEST, connection.getId());
    }
}
