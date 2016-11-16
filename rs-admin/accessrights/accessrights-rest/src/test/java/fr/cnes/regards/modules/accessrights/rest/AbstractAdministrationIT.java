/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ImportResource;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.project.dao.IProjectRepository;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class AbstractAdministrationIT
 *
 * Abstract class for all administration integration tets.
 *
 * @author sbinda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@ImportResource({ "classpath*:defaultRoles.xml" })
public abstract class AbstractAdministrationIT extends AbstractRegardsIT {

    /**
     * Test project name
     */
    public static final String PROJECT_TEST_NAME = "test1";

    /**
     * Project Repository STUB
     */
    @Autowired
    private IProjectRepository projectRepository;

    /**
     * Method authorization service.
     */
    @Autowired
    private MethodAuthorizationService methodAuthorizationService;

    @Before
    public void initProjects() {
        // Clean repository
        projectRepository.deleteAll();

        final Project project = new Project(0L, "desc", "icon", true, PROJECT_TEST_NAME);
        projectRepository.save(project);

        // Refresh method autorization service after add the project
        methodAuthorizationService.refreshAuthorities();

        init();
    }

    protected abstract void init();

}
