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
package fr.cnes.regards.modules.project.rest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectsControllerIT
 *
 * Tests for REST endpoints to access Project entities.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@InstanceTransactional
public class ProjectsControllerIT extends AbstractRegardsIT {

    private final static Logger LOG = LoggerFactory.getLogger(ProjectsControllerIT.class);

    /**
     * JWT service
     */
    @Autowired
    protected JWTService jwtService;

    /**
     * Instance admin token
     */
    private String instanceAdmintoken;

    /**
     * Public Token
     */
    private String publicToken;

    @Autowired
    private IProjectRepository projectRepo;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Before
    public void initialize() {
        instanceAdmintoken = jwtService.generateToken("test1", DEFAULT_USER_EMAIL, DefaultRole.INSTANCE_ADMIN.name());
        publicToken = jwtService.generateToken("test1", DEFAULT_USER_EMAIL, DefaultRole.PUBLIC.name());
    }

    /**
     *
     * Check REST Access to project resources and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrievePublicProjectsTest() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet("/projects/public", publicToken, requestBuilderCustomizer, "error");
    }

    /**
     *
     * Check REST Access to project resources and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrieveAllProjectsByPage() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.size", Matchers.is(1)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                                                        .jsonPath(JSON_PATH_ROOT + ".metadata.totalElements",
                                                                  Matchers.is(3)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.totalPages",
                                                                               Matchers.is(3)));
        performGet("/projects?page={page}&size={size}",
                   instanceAdmintoken,
                   requestBuilderCustomizer,
                   "Error there must be project results",
                   "0",
                   "1");
    }

    /**
     *
     * Check REST Access to project resources and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrieveAllProjects() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.size", Matchers.is(20)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                                                        .jsonPath(JSON_PATH_ROOT + ".metadata.totalElements",
                                                                  Matchers.is(3)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.totalPages",
                                                                               Matchers.is(1)));
        performGet("/projects", instanceAdmintoken, requestBuilderCustomizer, "Error there must be project results");
    }

    /**
     *
     * Check REST Access to project resource and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resource and Hateoas returned links")
    @Test
    public void retrieveProjectTest() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet("/projects/test1", publicToken, requestBuilderCustomizer, "Error there must be project results");
    }

    /**
     *
     * Check REST Access for project creation and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access for project creation and Hateoas returned links")
    @Test
    public void createProjectTest() {

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        Project project = new Project("description", "icon", true, "create-project");
        project.setLabel("create-project");
        performPost("/projects",
                    instanceAdmintoken,
                    project,
                    requestBuilderCustomizer,
                    "Error there must be project results");
    }

    @Test
    public void createTwoProjectWithDifferentCase() {

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        Project project = new Project("description", "icon", true, "create-project");
        project.setLabel("create-project");
        performPost("/projects",
                    instanceAdmintoken,
                    project,
                    requestBuilderCustomizer,
                    "Error there must be project results");

        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isConflict());
        project = new Project("description", "icon", true, "creAte-project");
        project.setLabel("create-project");
        performPost("/projects",
                    instanceAdmintoken,
                    project,
                    requestBuilderCustomizer,
                    "Error there must be project results");

    }

    /**
     *
     * Check REST Access for project update and Hateoas returned links
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access for project update and Hateoas returned links")
    @Test
    public void updateProjectTest() {
        final Project project = projectRepo.findOneByNameIgnoreCase("test1");
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performPut("/projects/" + project.getName(),
                   instanceAdmintoken,
                   project,
                   requestBuilderCustomizer,
                   "Error there must be project results");
    }

}
