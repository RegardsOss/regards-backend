/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.instance.transactional.InstanceTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
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
     * Instance admin token
     */
    private String instanceAdmintoken;

    /**
     * Public Token
     */
    private String publicToken;

    /**
     * JWT service
     */
    @Autowired
    protected JWTService jwtService;

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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet("/projects/public", publicToken, expectations, "error");
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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.size", Matchers.is(1)));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.totalElements", Matchers.is(3)));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.totalPages", Matchers.is(3)));
        performGet("/projects?page={page}&size={size}", instanceAdmintoken, expectations,
                   "Error there must be project results", "0", "1");
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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.size", Matchers.is(20)));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.totalElements", Matchers.is(3)));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT + ".metadata.totalPages", Matchers.is(1)));
        performGet("/projects", instanceAdmintoken, expectations, "Error there must be project results");
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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet("/projects/test1", publicToken, expectations, "Error there must be project results");
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

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isCreated());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performPost("/projects", instanceAdmintoken, new Project("description", "icon", true, "create-project"),
                    expectations, "Error there must be project results");
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
        final Project project = projectRepo.findOneByName("test1");
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performPut("/projects/" + project.getName(), instanceAdmintoken, project, expectations,
                   "Error there must be project results");
    }

}
