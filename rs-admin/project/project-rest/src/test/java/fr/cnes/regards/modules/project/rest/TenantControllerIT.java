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
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.project.dao.IProjectConnectionRepository;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;
import fr.cnes.regards.modules.project.domain.ProjectConnection;

/**
*
* Class TenantControllerIT
*
* Tests for REST endpoints to access tenant entities.
*
* @author Sébastien Binda
* @since 1.0-SNAPSHOT
*/
@InstanceTransactional
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
        instanceAdmintoken = jwtService.generateToken("test1", DEFAULT_USER_EMAIL, DefaultRole.INSTANCE_ADMIN.name());

        Project activeProject = new Project("description", "icon", true, ACTIVE_PROJECT_NAME);
        activeProject.setLabel("label");
        Project deletedProject = new Project("description", "icon", true, "deletedProject");
        deletedProject.setDeleted(true);
        deletedProject.setLabel("label");

        ProjectConnection rsTestConnection = new ProjectConnection(activeProject, TEST_MS, "user", "password", "driver",
                "url");
        rsTestConnection.setEnabled(true);
        ProjectConnection rsTestConnection2 = new ProjectConnection(deletedProject, TEST_MS, "user", "password",
                "driver", "url");
        rsTestConnection2.setEnabled(true);

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
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.hasSize(1)));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT, Matchers.contains(ACTIVE_PROJECT_NAME)));
        performGet(TenantController.BASE_PATH + TenantController.MICROSERVICE_PATH, instanceAdmintoken, expectations,
                   "error", TEST_MS);
    }

}
