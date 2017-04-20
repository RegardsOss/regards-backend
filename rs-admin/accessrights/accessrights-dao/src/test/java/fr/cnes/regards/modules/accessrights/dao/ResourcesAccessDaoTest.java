/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 *
 * Class ResourcesAccessDaoTest
 *
 * Test class for DAO entities ResourcesAccess
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { AccessRightsDaoTestConfiguration.class })
@MultitenantTransactional
public class ResourcesAccessDaoTest {

    private static final String MS_NAME = "rs-test";

    private static final String USER_URL = "/user";

    private static final String PUBLIC_URL = "/public";

    private static final String ADMIN_URL = "/admin";

    private Role publicRole;

    private Role userRole;

    private Role adminRole;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IResourcesAccessRepository resourceAccessRepository;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test1");
    }

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     *
     * Initialize repository datas
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {

        /*
         * Create 3 ResourcesAcces
         */
        ResourcesAccess publicResource = new ResourcesAccess("Public resource", MS_NAME, PUBLIC_URL, "controller",
                RequestMethod.GET, DefaultRole.PUBLIC);

        ResourcesAccess userResource = new ResourcesAccess("User resource", MS_NAME, USER_URL, "controller",
                RequestMethod.GET, DefaultRole.REGISTERED_USER);

        ResourcesAccess adminResource = new ResourcesAccess("Admin resource", MS_NAME, ADMIN_URL, "controller",
                RequestMethod.GET, DefaultRole.PROJECT_ADMIN);

        publicResource = resourceAccessRepository.save(publicResource);
        userResource = resourceAccessRepository.save(userResource);
        adminResource = resourceAccessRepository.save(adminResource);
        /*
         * Create 3 Role
         */
        publicRole = new Role(DefaultRole.PUBLIC.toString(), null);
        publicRole.setNative(true);
        publicRole.addPermission(publicResource);
        publicRole = roleRepository.save(publicRole);

        userRole = new Role(DefaultRole.REGISTERED_USER.toString(), publicRole);
        userRole.setNative(true);

        userRole.addPermission(publicResource);
        userRole.addPermission(userResource);
        userRole = roleRepository.save(userRole);

        adminRole = new Role(DefaultRole.ADMIN.toString(), userRole);
        adminRole.setNative(true);

        adminRole.addPermission(publicResource);
        adminRole.addPermission(userResource);
        adminRole.addPermission(adminResource);
        adminRole = roleRepository.save(adminRole);
    }

    @Test
    public void findByParentRoleName() {
        final Set<Role> roles = roleRepository.findByParentRoleName(DefaultRole.PUBLIC.toString());
        Assert.assertNotNull(roles);
        Assert.assertEquals(1, roles.size());
        Assert.assertNotNull(((Role) roles.toArray()[0]).getPermissions());
        Assert.assertTrue(((Role) roles.toArray()[0]).getPermissions().size() > 0);
    }

}
