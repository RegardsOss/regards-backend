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
package fr.cnes.regards.modules.project.service;

import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 * Class ProjectServiceTest
 *
 * Project business service tests
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @author Olivier Rousselot
 */
@PropertySource("classpath:application-test.properties")
public class ProjectConnectionServiceTest extends AbstractRegardsServiceIT {

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_DESCRIPTION = "description";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_ICON = "icon";

    /**
     * Common string value for project creation.
     */
    private static final String PROJECT_TEST_1 = "project-test-1";

    /**
     * Common string value for project creation.
     */
    private static final String PROJECT_TEST_2 = "project-test-2";

    /**
     * Common string value for project creation.
     */
    private static final String PROJECT_TEST_3 = "project-test-3";

    /**
     * Common string value for project creation.
     */
    private static final String MS_TEST_1 = "ms-test-1";

    /**
     * Common string value for project creation.
     */
    private static final String MS_TEST_2 = "ms-test-2";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_USER_NAME = "username";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_USER_PWD = "password";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_DRIVER = "driver";

    private static final String PROJECT1_URL = "url1";

    private static final String PROJECT2_URL = "url2";

    @Autowired
    private IProjectService projectService;

    @Autowired
    private IProjectConnectionService projectConnectionService;

    @Autowired
    private IProjectConnectionRepository projectConnectionRepo;

    @Autowired
    private IProjectRepository projectRepo;

    private Project project1;

    private Project project2;

    private ProjectConnection projectCtx;

    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        projectConnectionRepo.deleteAll();
        projectRepo.deleteAll();

        AESEncryptionService aesEncryptionService = new AESEncryptionService();
        aesEncryptionService
                .init(new CipherProperties(Paths.get("src", "test", "resources", "testKey"), "1234567812345678"));

        project1 = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_1);
        project1.setLabel("Project1");
        project1 = projectRepo.save(project1);

        project2 = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_2);
        project2.setLabel("Project2");
        project2 = projectRepo.save(project2);

        projectCtx = new ProjectConnection(project1, MS_TEST_1, COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD,
                COMMON_PROJECT_DRIVER, PROJECT1_URL);
        projectCtx = projectConnectionRepo.save(projectCtx);
        projectConnectionRepo.save(new ProjectConnection(project2, MS_TEST_2, COMMON_PROJECT_USER_NAME,
                COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT2_URL));
        projectConnectionRepo.save(new ProjectConnection(project2, "ms-test-3", COMMON_PROJECT_USER_NAME,
                COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT2_URL));

    }

    /**
     * Test creation of a new database connection for a given project and a given microservice
     * @throws ModuleException if error occurs!
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test creation of a new database connection for a given project and a given microservice.")
    @Test
    public void createProjectConnection() throws ModuleException {

        Project project1 = projectService.retrieveProject(PROJECT_TEST_1);

        // Test database parameter conflict detection : project 1 connection = project 2 connection
        ProjectConnection connection = new ProjectConnection(600L, project1, "microservice-test",
                COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT2_URL);
        try {
            projectConnectionService.createProjectConnection(connection, true);
            Assert.fail("Conflicting connection should not be created");
        } catch (EntityInvalidException e) {
            // Nothing to do
        }

        // Test database parameter conflict detection : project 1 connection on MS_TEST_1 = project 1 connection on
        // microservice-test
        connection.setUrl(PROJECT1_URL);
        projectConnectionService.createProjectConnection(connection, true);

        try {
            projectConnectionService.createProjectConnection(connection, true);
            Assert.fail("Impossible to add two project connection for same project and microservice");
        } catch (final EntityAlreadyExistsException e) {
            // Noting to do
        }
    }

    /**
     * Test deletion of a database connection for a given project and a given microservice.
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test deletion of a database connection for a given project and a given microservice.")
    @Test
    public void deleteProjectConnection() {
        ProjectConnection connection = null;
        try {
            connection = projectConnectionService.retrieveProjectConnection(PROJECT_TEST_2, MS_TEST_2);
        } catch (final EntityNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        try {
            projectConnectionService.deleteProjectConnection(connection.getId());
        } catch (final EntityNotFoundException e1) {
            Assert.fail(e1.getMessage());
        }

        try {
            connection = projectConnectionService.retrieveProjectConnection(PROJECT_TEST_2, MS_TEST_1);
            Assert.fail("Deletion error. Project connection always exists.");
        } catch (final EntityNotFoundException e1) {
            // Nothing to do
        }

        try {
            final long id = 5556L;
            projectConnectionService.deleteProjectConnection(id);
            Assert.fail("Error the deletion should be in error. The entity doest not exists.");
        } catch (final EntityNotFoundException e) {
            // Nothing to do
        }
    }

    @Test
    public void testProjectDeletion() throws ModuleException {

        Page<ProjectConnection> page = projectConnectionRepo.findByProjectName(PROJECT_TEST_2, Pageable.unpaged());
        Assert.assertTrue(page.getTotalElements() == 2);

        projectService.deleteProject(PROJECT_TEST_2);

        page = projectConnectionRepo.findByProjectName(PROJECT_TEST_2, Pageable.unpaged());
        Assert.assertTrue(page.isEmpty());
    }

    /**
     * Test updating of a database connection for a given project and a given microservice.
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test updating of a database connection for a given project and a given microservice.")
    @Test
    public void updateProjectConnection() {

        final String updateUserName = "newUser";
        final String errorUpdate = "Error the update should be in error. The entity doest not exists.";
        ProjectConnection connection = null;
        try {
            connection = projectConnectionService.retrieveProjectConnection(PROJECT_TEST_1, MS_TEST_1);
        } catch (final EntityNotFoundException e) {
            Assert.fail(e.getMessage());
        }
        connection.setUserName(updateUserName);
        try {
            connection = projectConnectionService.updateProjectConnection(connection.getId(), connection);
            Assert.assertEquals("Error updating project connection.", connection.getUserName(), updateUserName);
        } catch (ModuleException e1) {
            Assert.fail(e1.getMessage());
        }

        // Updating with an non existing project
        connection = new ProjectConnection(0L,
                new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_3), MS_TEST_1,
                COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT1_URL);
        try {
            connection = projectConnectionService.updateProjectConnection(0L, connection);
            Assert.fail(errorUpdate);
        } catch (ModuleException e) {
            // Nothing to do
        }

        // Updating a non existing projectConnection
        final long id = 56L;
        connection = new ProjectConnection(id,
                new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_3), MS_TEST_1,
                COMMON_PROJECT_USER_NAME, COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT1_URL);
        try {
            connection = projectConnectionService.updateProjectConnection(id, connection);
            Assert.fail(errorUpdate);
        } catch (ModuleException e) {
            // Nothing to do
        }
    }

    /**
     * Test to retrieve projects connections of given project's name in instance database.
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose(" Test to retrieve projects connections of given project's name in instance database.")
    @Test
    public void testRetrieveProjectsConnectionsByProject() {
        final Pageable pageable = PageRequest.of(0, 100);

        // Call tested method
        final Page<ProjectConnection> actual = projectConnectionService
                .retrieveProjectsConnectionsByProject(PROJECT_TEST_1, pageable);

        // Check
        Assert.assertEquals(1, actual.getTotalElements());
    }

    /**
     * Test to retrieve projects connections of passed id in instance database.
     * @throws EntityNotFoundException No project connection with passed id
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test to retrieve projects connections of passed id in instance database.")
    @Test
    public void testRetrieveProjectConnectionById() throws EntityNotFoundException {

        // Call tested method
        ProjectConnection actual = projectConnectionService.retrieveProjectConnectionById(projectCtx.getId());

        // Check
        Assert.assertNotNull(actual);
    }
}
