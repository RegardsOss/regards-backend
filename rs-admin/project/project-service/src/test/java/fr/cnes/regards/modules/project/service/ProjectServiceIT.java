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
package fr.cnes.regards.modules.project.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

/**
 * Class ProjectServiceIT
 * <p>
 * Project business service tests
 *
 * @author Sébastien Binda
 */
@PropertySource("classpath:application-test.properties")
public class ProjectServiceIT extends AbstractRegardsServiceIT {

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

    @Autowired
    private IProjectService projectService;

    @Autowired
    private IProjectRepository projectRepo;

    @Autowired
    private IProjectConnectionRepository projectConRepo;

    private Project project1;

    private Project project2;

    @Before
    public void init() {
        projectConRepo.deleteAll();
        projectRepo.deleteAll();

        project1 = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, PROJECT_TEST_1);
        project1.setLabel("project1");
        projectRepo.save(project1);

        project2 = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, false, PROJECT_TEST_2);
        project2.setLabel("project2");
        projectRepo.save(project2);
    }

    /**
     * Check that the system allows to create a project.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Purpose("Check that the system allows to create a project.")
    public void createProjectTest() {
        Project projectToCreate = new Project(project1.getId(),
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
        projectToCreate = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, false, "new-project-test");
        projectToCreate.setLabel("projectToCreate");
        try {
            projectService.createProject(projectToCreate);
        } catch (final ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Check that the system allows to retrieve all projects for an instance.
     */
    @Test
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Purpose("Check that the system allows to retrieve all projects for an instance.")
    public void retrieveAllProjectTest() {
        final List<Project> projects = projectService.retrieveProjectList();
        Assert.assertTrue("There must be projects.", !projects.isEmpty());
    }

    /**
     * Check that the system allows to retrieve a project on an instance and handle fail cases.
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
     * Check that the system allows to update a project on an instance and handle fail cases.
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
