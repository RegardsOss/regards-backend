/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ImportResource;

import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.modules.accessrights.dao.instance.IAccountRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class AbstractAdministrationIT
 *
 * Abstract class for all administration integration tets.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @since 1.0-SNAPSHOT
 */
@ImportResource({ "classpath*:defaultRoles.xml" })
public abstract class AbstractAdministrationIT extends AbstractRegardsIT {

    /**
     * Test project name
     */
    public static final String PROJECT_TEST_NAME = "test1";

    protected static String token;

    protected static final String ROLE_TEST = "TEST_ROLE";

    protected Role roleTest;

    protected Role publicRole;

    /**
     * Role repository
     */
    @Autowired
    private IRoleRepository roleRepository;

    /**
     * Project Repository STUB
     */
    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Project Connection Repository STUB
     */
    @Autowired
    private IAccountRepository accountRepository;

    @Before
    public void initProjects() {
        token = jwtService.generateToken(AbstractAdministrationIT.PROJECT_TEST_NAME, "email", ROLE_TEST, ROLE_TEST);
        jwtService.injectMockToken(AbstractAdministrationIT.PROJECT_TEST_NAME, DefaultRole.PUBLIC.toString());

        // Clear the repos
        projectUserRepository.deleteAll();
        accountRepository.deleteAll();

        // Refresh method autorization service after add the project
        // methodAuthorizationService.refreshAuthorities();

        // Init roles
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        roleRepository.findOneByName(ROLE_TEST).ifPresent(role -> roleRepository.delete(role));
        roleTest = roleRepository.save(new Role(ROLE_TEST, publicRole));

        init();
    }

    protected abstract void init();

}
