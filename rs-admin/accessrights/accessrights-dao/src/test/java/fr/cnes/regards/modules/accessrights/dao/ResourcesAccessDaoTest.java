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

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
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

    /**
     *
     * Initialize repository datas
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {

        /*
         * Create 3 Role
         */
        publicRole = new Role(DefaultRole.PUBLIC.toString(), null);
        userRole = new Role(DefaultRole.REGISTERED_USER.toString(), publicRole);
        adminRole = new Role(DefaultRole.ADMIN.toString(), userRole);

        /*
         * Create 3 ResourcesAcces
         */
        final ResourcesAccess publicResource = new ResourcesAccess("Public resource", MS_NAME, PUBLIC_URL,
                HttpVerb.GET);

        final ResourcesAccess userResource = new ResourcesAccess("User resource", MS_NAME, USER_URL, HttpVerb.GET);

        final ResourcesAccess adminResource = new ResourcesAccess("Admin resource", MS_NAME, ADMIN_URL, HttpVerb.GET);

        /*
         * Set Permission to Role and persist the Role
         */
        publicRole.setPermissions(Sets.newHashSet(publicResource));
        roleRepository.save(publicRole);

        userRole.setPermissions(Sets.newHashSet(publicResource, userResource));
        roleRepository.save(userRole);

        adminRole.setPermissions(Sets.newHashSet(publicResource, userResource, adminResource));
        roleRepository.save(adminRole);
    }

    @Test
    public void findByParentRoleName() {
        Set<Role> roles = roleRepository.findByParentRoleName(DefaultRole.PUBLIC.toString());
        Assert.assertNotNull(roles);
        Assert.assertEquals(1, roles.size());
        Assert.assertNotNull(((Role) roles.toArray()[0]).getPermissions());
        Assert.assertTrue(((Role) roles.toArray()[0]).getPermissions().size() > 0);
    }

}
