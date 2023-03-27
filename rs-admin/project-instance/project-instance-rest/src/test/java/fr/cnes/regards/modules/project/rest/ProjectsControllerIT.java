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

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

/**
 * Class ProjectsControllerIT
 * <p>
 * Tests for REST endpoints to access Project entities.
 *
 * @author Sébastien Binda
 */
@InstanceTransactional
@ContextConfiguration(classes = { LicenseConfiguration.class })
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

    @Autowired
    private IProjectRepository projectRepo;

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Before
    public void initialize() {
        instanceAdmintoken = jwtService.generateToken("test1",
                                                      getDefaultUserEmail(),
                                                      DefaultRole.INSTANCE_ADMIN.name());
    }

    /**
     * Check REST Access to project resources and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrievePublicProjectsTest() {
        performGet("/projects/public",
                   instanceAdmintoken,
                   customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT),
                   "error");
    }

    /**
     * Check REST Access to project resources and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrieveAllProjectsByPage() {
        performGet("/projects?page={page}&size={size}",
                   instanceAdmintoken,
                   customizer().expectStatusOk()
                               .expectIsNotEmpty(JSON_PATH_ROOT)
                               .expectValue(JSON_PATH_ROOT + ".metadata.size", 1)
                               .expectValue(JSON_PATH_ROOT + ".metadata.totalElements", 3)
                               .expectValue(JSON_PATH_ROOT + ".metadata.totalPages", 3),
                   "Error there must be project results",
                   "0",
                   "1");
    }

    /**
     * Check REST Access to project resources and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_130")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resources and returned Hateoas links")
    @Test
    public void retrieveAllProjects() {
        performGet("/projects",
                   instanceAdmintoken,
                   customizer().expectStatusOk()
                               .expectIsNotEmpty(JSON_PATH_ROOT)
                               .expectValue(JSON_PATH_ROOT + ".metadata.size", 20)
                               .expectValue(JSON_PATH_ROOT + ".metadata.totalElements", 3)
                               .expectValue(JSON_PATH_ROOT + ".metadata.totalPages", 1)
                               .addParameter("size", "20"),

                   "Error there must be project results");
    }

    /**
     * Check REST Access to project resource and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access to project resource and Hateoas returned links")
    @Test
    public void retrieveProjectTest() {
        performGet("/projects/test1",
                   instanceAdmintoken,
                   customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT),
                   "Error there must be project results");
    }

    /**
     * Check REST Access for project creation and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access for project creation and Hateoas returned links")
    @Test
    public void createProjectTest() {
        Project project = new Project("description", "icon", true, "create-project");
        project.setLabel("create-project");
        performPost("/projects",
                    instanceAdmintoken,
                    project,
                    customizer().expectStatusCreated().expectIsNotEmpty(JSON_PATH_ROOT),
                    "Error there must be project results");
    }

    @Test
    public void createTwoProjectWithDifferentCase() {
        Project project = new Project("description", "icon", true, "create-project");
        project.setLabel("create-project");
        performPost("/projects",
                    instanceAdmintoken,
                    project,
                    customizer().expectStatusCreated().expectIsNotEmpty(JSON_PATH_ROOT),
                    "Error there must be project results");

        project = new Project("description", "icon", true, "creAte-project");
        project.setLabel("create-project");
        performPost("/projects",
                    instanceAdmintoken,
                    project,
                    customizer().expectStatusConflict(),
                    "Error there must be project results");

    }

    /**
     * Check REST Access for project update and Hateoas returned links
     */
    @Requirement("REGARDS_DSL_ADM_INST_100")
    @Requirement("REGARDS_DSL_SYS_ARC_020")
    @Purpose("Check REST Access for project update and Hateoas returned links")
    @Test
    public void updateProjectTest() {
        Project project = projectRepo.findOneByNameIgnoreCase("test1");
        performPut("/projects/" + project.getName(),
                   instanceAdmintoken,
                   project,
                   customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT),
                   "Error there must be project results");
    }

}
