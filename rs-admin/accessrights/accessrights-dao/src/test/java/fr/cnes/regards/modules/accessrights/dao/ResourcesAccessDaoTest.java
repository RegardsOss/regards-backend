/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.dao;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
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

    /**
     * The number of {@link Role} used for the unit testing
     */
    private int nRole = 0;

    private Role publicRole;

    private Role userRole;

    private Role adminRole;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRespository;

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
        nRole++;

        userRole = new Role(DefaultRole.REGISTERED_USER.toString(), publicRole);
        nRole++;

        adminRole = new Role(DefaultRole.ADMIN.toString(), userRole);
        nRole++;

        /*
         * Create 3 ResourcesAcces
         */
        final ResourcesAccess publicResource = new ResourcesAccess("Public resource", MS_NAME, PUBLIC_URL,
                HttpVerb.GET);
        publicResource.addRole(publicRole);
        publicResource.addRole(userRole);
        publicResource.addRole(adminRole);

        final ResourcesAccess userResource = new ResourcesAccess("User resource", MS_NAME, USER_URL, HttpVerb.GET);
        userResource.addRole(userRole);
        userResource.addRole(adminRole);

        final ResourcesAccess adminResource = new ResourcesAccess("Admin resource", MS_NAME, ADMIN_URL, HttpVerb.GET);
        adminResource.addRole(adminRole);

        /*
         * Set Permission to Role and persist the Role
         */
        publicRole.setPermissions(Arrays.asList(publicResource));
        roleRepository.save(publicRole);

        userRole.setPermissions(Arrays.asList(publicResource, userResource));
        roleRepository.save(userRole);

        adminRole.setPermissions(Arrays.asList(publicResource, userResource, adminResource));
        roleRepository.save(adminRole);
    }

    /**
     *
     * Test to retrieve all resources with a list of roles
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testRetrieveResourcesByRole() {

        final List<String> publicRolesName = Arrays.asList(DefaultRole.PUBLIC.toString());
        final List<String> userRolesName = Arrays.asList(DefaultRole.PUBLIC.toString(),
                                                         DefaultRole.REGISTERED_USER.toString());
        final List<String> adminRolesName = Arrays.asList(DefaultRole.PUBLIC.toString(),
                                                          DefaultRole.REGISTERED_USER.toString(),
                                                          DefaultRole.ADMIN.toString());

        final Pageable pageable = new PageRequest(0, 10);

        final List<ResourcesAccess> allResources = resourcesAccessRespository.findByMicroservice(MS_NAME);
        Assert.assertNotNull(allResources);
        Assert.assertEquals(nRole, allResources.size());

        ResourcesAccess resourcesAccess = resourcesAccessRespository
                .findOneByMicroserviceAndResourceAndVerb(MS_NAME, USER_URL, HttpVerb.GET);
        Assert.assertNotNull(resourcesAccess);
        Assert.assertEquals(2, resourcesAccess.getRoles().size());
        Assert.assertTrue(resourcesAccess.getRoles().contains(userRole));
        Assert.assertTrue(resourcesAccess.getRoles().contains(adminRole));

        final List<ResourcesAccess> publicResources = resourcesAccessRespository
                .findDistinctByRolesNameIn(publicRolesName);
        Assert.assertNotNull(publicResources);
        Assert.assertEquals(1, publicResources.size());

        final Page<ResourcesAccess> publicResourcesPage = resourcesAccessRespository
                .findDistinctByMicroserviceAndRolesNameIn(MS_NAME, publicRolesName, pageable);
        Assert.assertNotNull(publicResourcesPage);
        Assert.assertNotNull(publicResourcesPage.getContent());
        Assert.assertEquals(1, publicResourcesPage.getTotalElements());
        Assert.assertEquals(1, publicResourcesPage.getContent().size());

        final Page<ResourcesAccess> userResourcesPage = resourcesAccessRespository
                .findDistinctByMicroserviceAndRolesNameIn(MS_NAME, userRolesName, pageable);
        Assert.assertNotNull(userResourcesPage);
        Assert.assertNotNull(userResourcesPage.getContent());
        Assert.assertEquals(2, userResourcesPage.getTotalElements());
        Assert.assertEquals(2, userResourcesPage.getContent().size());

        final Page<ResourcesAccess> adminResourcesPage = resourcesAccessRespository
                .findDistinctByMicroserviceAndRolesNameIn(MS_NAME, adminRolesName, pageable);
        Assert.assertNotNull(adminResourcesPage);
        Assert.assertNotNull(adminResourcesPage.getContent());
        Assert.assertEquals(3, adminResourcesPage.getTotalElements());
        Assert.assertEquals(3, adminResourcesPage.getContent().size());
    }

}
