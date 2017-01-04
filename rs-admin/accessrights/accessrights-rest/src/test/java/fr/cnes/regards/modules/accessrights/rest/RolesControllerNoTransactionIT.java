/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.RoleService;

/**
 * Integration tests for Roles REST Controller.
 *
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
public class RolesControllerNoTransactionIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RolesControllerNoTransactionIT.class);

    private String apiRoles;

    private String apiRolesId;

    private String apiRolesName;

    private String apiRolesPermissions;

    private String apiRolesUsers;

    @Autowired
    private RoleService roleService;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     * Role repository
     */
    @Autowired
    private IRoleRepository roleRepository;

    @Rule
    public ExpectedException thrown_ = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword;

    private static final String ROLE_TEST = "TEST_ROLE";

    private Role roleTest;

    private Role publicRole;

    @Before
    public void init() throws JwtException {
        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString());
        apiRoles = RolesController.REQUEST_MAPPING_ROOT;
        apiRolesId = apiRoles + "/{role_id}";
        apiRolesName = apiRoles + "/{role_name}";

        apiRolesPermissions = ResourcesController.REQUEST_MAPPING_ROOT + "/roles/{role_id}";

        apiRolesUsers = ProjectUsersController.REQUEST_MAPPING_ROOT + "/roles/{role_id}";

        // Init roles
        publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString()).get();
        final List<ResourcesAccess> resourcesAccessPublic = new ArrayList<>();
        final ResourcesAccess aResourcesAccessPublic = new ResourcesAccess("", "aMicroservice", "the public resource",
                HttpVerb.GET);
        aResourcesAccessPublic.setRoles(Arrays.asList(publicRole));
        resourcesAccessPublic.add(aResourcesAccessPublic);
        publicRole.setPermissions(resourcesAccessPublic);
        roleRepository.save(publicRole);

        // Create a new Role
        roleRepository.findOneByName(ROLE_TEST).ifPresent(role -> roleRepository.delete(role));
        final Role aNewRole = roleRepository.save(new Role(ROLE_TEST, publicRole));

        final List<ResourcesAccess> resourcesAccess = new ArrayList<>();
        final ResourcesAccess aResourcesAccess = new ResourcesAccess("", "aMicroservice", "the resource", HttpVerb.GET);
        final ResourcesAccess bResourcesAccess = new ResourcesAccess("", "aMicroservice", "the resource",
                HttpVerb.DELETE);
        aResourcesAccess.setRoles(Arrays.asList(roleRepository.findAll().get(0), aNewRole));
        bResourcesAccess.setRoles(Arrays.asList(aNewRole, roleRepository.findAll().get(1)));

        resourcesAccess.add(aResourcesAccess);
        resourcesAccess.add(bResourcesAccess);
        aNewRole.setPermissions(resourcesAccess);
        roleTest = roleRepository.save(aNewRole);
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_210")
    @Purpose("Check that the allows to retrieve roles.")
    public void retrieveRoleList() throws JwtException {
        Assert.assertEquals(roleRepository.count(), 6);
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.id", hasSize(6)));
        // 6 = 5 roles and the added role TEST_ROLE has two permissions
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.permissions", hasSize(6)));
        // 5 = 5 roles has a parent (public has no parent)
        expectations.add(MockMvcResultMatchers.jsonPath("$.*.content.parentRole", hasSize(5)));
        performDefaultGet(apiRoles, expectations, "TODO Error message");
    }

    @After
    public void rollback() throws JwtException {
        jwtService.injectToken(DEFAULT_TENANT, DefaultRole.PROJECT_ADMIN.toString());
        roleRepository.findOneByName(ROLE_TEST).ifPresent(role -> roleRepository.delete(role));
        Assert.assertEquals(roleRepository.count(), 5);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
