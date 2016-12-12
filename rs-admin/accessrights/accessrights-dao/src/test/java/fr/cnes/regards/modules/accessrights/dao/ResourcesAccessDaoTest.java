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

    @Autowired
    IRoleRepository roleRepository;

    @Autowired
    IResourcesAccessRepository resourcesAccessRespository;

    /**
     *
     * Initialize repository datas
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {

        Role publicRole = new Role("PUBLIC", null);
        publicRole = roleRepository.save(publicRole);

        Role userRole = new Role("USER", publicRole);
        userRole = roleRepository.save(userRole);

        Role adminRole = new Role("ADMIN", userRole);
        adminRole = roleRepository.save(adminRole);

        final ResourcesAccess publicResource = new ResourcesAccess("Public resource", MS_NAME, "/public", HttpVerb.GET);
        publicResource.addRole(publicRole);
        publicResource.addRole(userRole);
        publicResource.addRole(adminRole);
        resourcesAccessRespository.save(publicResource);

        final ResourcesAccess userResource = new ResourcesAccess("User resource", MS_NAME, "/user", HttpVerb.GET);
        userResource.addRole(userRole);
        userResource.addRole(adminRole);
        resourcesAccessRespository.save(userResource);

        final ResourcesAccess adminResource = new ResourcesAccess("Admin resource", MS_NAME, "/admin", HttpVerb.GET);
        adminResource.addRole(adminRole);
        resourcesAccessRespository.save(adminResource);

    }

    /**
     *
     * Test to retrieve all resources with a list of roles
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testRetrieveResourcesByRole() {

        final List<String> publicRolesName = Arrays.asList("PUBLIC");
        final List<String> userRolesName = Arrays.asList("PUBLIC", "USER");
        final List<String> adminRolesName = Arrays.asList("PUBLIC", "USER", "ADMIN");

        final Pageable pageable = new PageRequest(0, 10);

        final List<ResourcesAccess> allResources = resourcesAccessRespository.findByMicroservice(MS_NAME);
        Assert.assertNotNull(allResources);
        Assert.assertEquals(3, allResources.size());

        final Page<ResourcesAccess> publicResources = resourcesAccessRespository
                .findDistinctByMicroserviceAndRolesNameIn(MS_NAME, publicRolesName, pageable);
        Assert.assertNotNull(publicResources);
        Assert.assertNotNull(publicResources.getContent());
        Assert.assertEquals(1, publicResources.getTotalElements());
        Assert.assertEquals(1, publicResources.getContent().size());

        final Page<ResourcesAccess> userResources = resourcesAccessRespository
                .findDistinctByMicroserviceAndRolesNameIn(MS_NAME, userRolesName, pageable);
        Assert.assertNotNull(userResources);
        Assert.assertNotNull(userResources.getContent());
        Assert.assertEquals(2, userResources.getTotalElements());
        Assert.assertEquals(2, userResources.getContent().size());

        final Page<ResourcesAccess> adminResources = resourcesAccessRespository
                .findDistinctByMicroserviceAndRolesNameIn(MS_NAME, adminRolesName, pageable);
        Assert.assertNotNull(adminResources);
        Assert.assertNotNull(adminResources.getContent());
        Assert.assertEquals(3, adminResources.getTotalElements());
        Assert.assertEquals(3, adminResources.getContent().size());

    }

}
