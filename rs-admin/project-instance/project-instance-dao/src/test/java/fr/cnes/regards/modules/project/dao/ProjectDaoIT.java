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
package fr.cnes.regards.modules.project.dao;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * Class ProjectDaoTest
 * <p>
 * Test class for DAO of project module
 *
 * @author CS
 * @author Xavier-Alexandre Brochard
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
//@TestPropertySource(properties = { "spring.jpa.show-sql=true" })
@ContextConfiguration(classes = { ProjectDaoTestConfiguration.class })
public class ProjectDaoIT {

    /**
     * A microservce name
     */
    private static final String MICROSERVICE_1 = "microservice-test";

    /**
     * An other microservice name
     */
    private static final String MICROSERVICE_2 = "microservice-test-2";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_NAME_1 = "project-test";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_NAME_2 = "project-test-2";

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
    private static final String COMMON_PROJECT_USER_NAME = "username";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_USER_PWD = "password";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_DRIVER = "driver";

    /**
     * Common string value for project creation.
     */
    private static final String COMMON_PROJECT_URL = "url";

    /**
     * Project Repository
     */
    @Autowired
    private IProjectRepository projectRepository;

    /**
     * ProjectConnection Repository
     */
    @Autowired
    private IProjectConnectionRepository projectConnectionRepository;

    /**
     * A project initialized before each test
     */
    private Project project;

    @Before
    public void setUp() {
        // First clean all elements from databse
        projectConnectionRepository.deleteAll();
        projectRepository.deleteAll();

        Project project1 = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, COMMON_PROJECT_NAME_1);
        project1.setLabel("project1");
        Project project2 = new Project(COMMON_PROJECT_DESCRIPTION, COMMON_PROJECT_ICON, true, COMMON_PROJECT_NAME_2);
        project2.setLabel("project2");
        // Create a new projects
        project = projectRepository.save(project1);
        projectRepository.save(project2);

        // Check results
        final Iterable<Project> projects = projectRepository.findAll();
        final List<Project> results = new ArrayList<>();
        projects.forEach(results::add);
        Assert.assertEquals(String.format("There must be 2 projects in database not %d", results.size()),
                            2,
                            results.size());

        // Create new projects connections
        projectConnectionRepository.save(new ProjectConnection(project,
                                                               MICROSERVICE_1,
                                                               COMMON_PROJECT_USER_NAME,
                                                               COMMON_PROJECT_USER_PWD,
                                                               COMMON_PROJECT_DRIVER,
                                                               COMMON_PROJECT_URL));
        projectConnectionRepository.save(new ProjectConnection(project,
                                                               MICROSERVICE_2,
                                                               COMMON_PROJECT_USER_NAME,
                                                               COMMON_PROJECT_USER_PWD,
                                                               COMMON_PROJECT_DRIVER,
                                                               COMMON_PROJECT_URL));
    }

    /**
     * Test to create and retrieve projects connections in instance database
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Test to create and retrieve projects connections in instance database.")
    @Test
    public void createAndRetreiveProjectConnections() {

        // Check results
        final Iterable<ProjectConnection> connections = projectConnectionRepository.findAll();
        final List<ProjectConnection> cresults = new ArrayList<>();
        connections.forEach(cresults::add);
        Assert.assertEquals(String.format("There must be 2 project connection in database not %d", cresults.size()),
                            2,
                            cresults.size());
        final ProjectConnection conn = projectConnectionRepository.findOneByProjectNameAndMicroservice(project.getName(),
                                                                                                       MICROSERVICE_1);
        final String errorMessage = "Error retreiving project connection for project name %s and microservice %s";
        Assert.assertNotNull(String.format(errorMessage, project.getName(), MICROSERVICE_1), conn);
        Assert.assertEquals("Error retreiving project connection for project name %s and microservice %s.",
                            conn.getMicroservice(),
                            MICROSERVICE_1);
    }

    /**
     * Test to retrieve projects connections of given project's name in instance database.
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose(" Test to retrieve projects connections of given project's name in instance database.")
    @Test
    public void testFindByProjectName() {
        final Pageable pageable = PageRequest.of(0, 100);

        final Page<ProjectConnection> result = projectConnectionRepository.findByProjectName(COMMON_PROJECT_NAME_1,
                                                                                             pageable);
        // Check
        final ProjectConnection connection0 = result.getContent().get(0);
        final ProjectConnection connection1 = result.getContent().get(1);
        Assert.assertEquals(2, result.getTotalElements());
        Assert.assertEquals(connection0.getProject(), project);
        Assert.assertEquals(connection0.getMicroservice(), MICROSERVICE_1);
        Assert.assertEquals(connection0.getUserName(), COMMON_PROJECT_USER_NAME);
        Assert.assertEquals(connection0.getPassword(), COMMON_PROJECT_USER_PWD);
        Assert.assertEquals(connection0.getDriverClassName(), COMMON_PROJECT_DRIVER);
        Assert.assertEquals(connection0.getUrl(), COMMON_PROJECT_URL);
        Assert.assertEquals(connection1.getProject(), project);
        Assert.assertEquals(connection1.getMicroservice(), MICROSERVICE_2);
        Assert.assertEquals(connection1.getUserName(), COMMON_PROJECT_USER_NAME);
        Assert.assertEquals(connection1.getPassword(), COMMON_PROJECT_USER_PWD);
        Assert.assertEquals(connection1.getDriverClassName(), COMMON_PROJECT_DRIVER);
        Assert.assertEquals(connection1.getUrl(), COMMON_PROJECT_URL);
    }

}
