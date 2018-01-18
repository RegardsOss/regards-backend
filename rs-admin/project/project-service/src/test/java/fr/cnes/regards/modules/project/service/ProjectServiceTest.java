/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import fr.cnes.regards.framework.amqp.IInstancePublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.dao.stub.ProjectRepositoryStub;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectServiceTest
 *
 * Project business service tests
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public class ProjectServiceTest {

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
     * Project service to test.
     */
    private ProjectService projectService;

    /**
     *
     * Initializa DAO Stub and inline entities for tests
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {

        // use a stub repository, to be able to only test the service
        final IProjectRepository projectRepoStub = new ProjectRepositoryStub();
        projectService = new ProjectService(projectRepoStub,
                                            Mockito.mock(ITenantResolver.class),
                                            Mockito.mock(IInstancePublisher.class),
                                            "default-project-test");

        projectRepoStub.save(new Project(0L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_1));
        projectRepoStub.save(new Project(1L, COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, false, PROJECT_TEST_2));
    }

    /**
     *
     * Check that the system allows to create a project.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to create a project.")
    public void createProjectTest() {
        final long newProjectId = 2L;
        Project projectToCreate = new Project(newProjectId,
                                              COMMON_PROJECT_DESCRIPTION,
                                              COMMON_PROJECT_ICON,
                                              false,
                                              PROJECT_TEST_1);
        try {
            projectService.createProject(projectToCreate);
            Assert.fail("Project already exists there must be an exception thrown here");
        } catch (final ModuleException e) {
            /// Nothing to do
        }
        projectToCreate = new Project(newProjectId,
                                      COMMON_PROJECT_DESCRIPTION,
                                      COMMON_PROJECT_ICON,
                                      false,
                                      "new-project-test");
        try {
            projectService.createProject(projectToCreate);
        } catch (final ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the system allows to retrieve all projects for an instance.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Purpose("Check that the system allows to retrieve all projects for an instance.")
    public void retrieveAllProjectTest() {
        final List<Project> projects = projectService.retrieveProjectList();
        Assert.assertTrue("There must be projects.", !projects.isEmpty());
    }

    /**
     *
     * Check that the system allows to retrieve a project on an instance and handle fail cases.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to retrieve a project on an instance and handle fail cases.")
    public void getProjectTest() {
        try {
            projectService.retrieveProject("invalid_project_name");
        } catch (final ModuleException e) {
            // Nothing to do
        }

        try {
            projectService.retrieveProject(PROJECT_TEST_1);
        } catch (final ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check that the system allows to update a project on an instance and handle fail cases.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to update a project on an instance and handle fail cases.")
    public void updateProject() {

        final String invalidProjectName = "project-invalid-update";
        final Project invalidProject = new Project(COMMON_PROJECT_DESCRIPTION,
                                                   COMMON_PROJECT_ICON,
                                                   false,
                                                   invalidProjectName);
        try {
            projectService.updateProject(invalidProjectName, invalidProject);
        } catch (final ModuleException e) {
            Assert.assertTrue(true);
        }
        Project existingProject = null;
        try {
            existingProject = projectService.retrieveProject(PROJECT_TEST_1);
            existingProject.setIcon("new-icon-update");
        } catch (final ModuleException e1) {
            Assert.fail(e1.getMessage());
        }

        try {
            projectService.updateProject(invalidProjectName, existingProject);
        } catch (final EntityNotFoundException e) {
            Assert.assertTrue(true);
        } catch (final ModuleException e) {
            Assert.fail(e.getMessage());
        }

        try {
            projectService.updateProject(PROJECT_TEST_1, existingProject);
        } catch (final ModuleException e) {
            Assert.fail(e.getMessage());
        }
        Assert.assertNotNull("The project to update exists. The returned project shouldn't be null", existingProject);

    }

}
