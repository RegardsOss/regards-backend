/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.persistence.EntityManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.encryption.AESEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectConnectionRepositoryStub;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
 *
 * Class ProjectServiceTest
 *
 * Project business service tests
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
public class ProjectConnectionServiceTest {

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

    /**
     * Project service to test.
     */
    private ProjectService projectService;

    /**
     * Project service to test.
     */
    private ProjectConnectionService projectConnectionService;

    /**
     * Stubbed repository
     */
    private IProjectConnectionRepository projectConnectionRepoStub;

    /**
     *
     * Initializa DAO Stub and inline entities for tests
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        // use a stub repository, to be able to only test the service
        IProjectRepository projectRepoStub = new ProjectRepositoryStub();
        projectService = new ProjectService(projectRepoStub, Mockito.mock(ITenantResolver.class),
                Mockito.mock(IInstancePublisher.class), "default-project-test",
                "http://localhost/default-project-test");
        AESEncryptionService aesEncryptionService = new AESEncryptionService();
        aesEncryptionService
                .init(new CipherProperties(Paths.get("src", "test", "resources", "testKey"), "1234567812345678"));
        projectConnectionRepoStub = new ProjectConnectionRepositoryStub();
        projectConnectionService = new ProjectConnectionService(projectRepoStub, projectConnectionRepoStub,
                Mockito.mock(IInstancePublisher.class), Mockito.mock(EntityManager.class), aesEncryptionService);

        Project project1 = projectRepoStub
                .save(new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_1));
        Project project2 = projectRepoStub
                .save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_2));

        projectConnectionRepoStub.save(new ProjectConnection(0L, project1, MS_TEST_1, COMMON_PROJECT_USER_NAME,
                COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT1_URL));
        projectConnectionRepoStub.save(new ProjectConnection(1L, project2, MS_TEST_2, COMMON_PROJECT_USER_NAME,
                COMMON_PROJECT_USER_PWD, COMMON_PROJECT_DRIVER, PROJECT2_URL));
    }

    /**
     *
     * Test creation of a new database connection for a given project and a given microservice
     * @throws ModuleException if error occurs!
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test creation of a new database connection for a given project and a given microservice.")
    @Test
    public void createProjectConnection() throws ModuleException, BadPaddingException, IllegalBlockSizeException {

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
     *
     * Test deletion of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
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

    /**
     *
     * Test updating of a database connection for a given project and a given microservice.
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test updating of a database connection for a given project and a given microservice.")
    @Test
    public void updateProjectConnection() throws BadPaddingException, IllegalBlockSizeException {

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
            Assert.assertTrue("Error updating project connection.", connection.getUserName().equals(updateUserName));
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
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose(" Test to retrieve projects connections of given project's name in instance database.")
    @Test
    public void testRetrieveProjectsConnectionsByProject() {
        final Pageable pageable = new PageRequest(0, 100);

        // Call tested method
        final Page<ProjectConnection> actual = projectConnectionService
                .retrieveProjectsConnectionsByProject(PROJECT_TEST_1, pageable);

        // Check
        Assert.assertEquals(1, actual.getTotalElements());
    }

    /**
     * Test to retrieve projects connections of passed id in instance database.
     *
     * @throws EntityNotFoundException
     *             No project connection with passed id
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test to retrieve projects connections of passed id in instance database.")
    @Test
    public void testRetrieveProjectConnectionById() throws EntityNotFoundException {

        // Call tested method
        final ProjectConnection actual = projectConnectionService.retrieveProjectConnectionById(0L);

        // Check
        Assert.assertNotNull(actual);
    }
}
