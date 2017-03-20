/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.resources.ResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 *
 * Class ResourcesServiceTest
 *
 * Test for resources management
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
 *
 * @since 1.0-SNAPSHOT
 */
public class ResourcesServiceTest {

    /**
     * Service to test
     */
    private ResourcesService resourcesService;

    /**
     * Mock for eureka discovery clients
     */
    private DiscoveryClient discoveryClientMock;

    /**
     * Mock to manage role entities
     */
    private IRoleService roleServiceMock;

    /**
     * Mock to manage projects resolver
     */
    private ITenantResolver tenantResolverMock;

    /**
     * Mock
     */
    private IRuntimeTenantResolver runtimeTenantResolverMock;

    /**
     * Stub for JPA Repository
     */
    private IResourcesAccessRepository resourcesRepo;

    private ResourcesAccess ra0;

    private ResourcesAccess ra1;

    private ResourcesAccess ra2;

    private ResourcesAccess ra3;

    private Set<ResourcesAccess> ras;

    private Role roleAdmin;

    /**
     *
     * Initialization of mocks and stubs
     *
     * @throws EntityNotFoundException
     *             test error
     *
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() throws EntityNotFoundException {

        resourcesRepo = Mockito.mock(IResourcesAccessRepository.class);
        ra0 = new ResourcesAccess(0L, "ResourceAccess 0", "Microservice 0", "Resource 0", "Controller", HttpVerb.GET);
        ra1 = new ResourcesAccess(1L, "ResourceAccess 1", "Microservice 1", "Resource 1", "Controller", HttpVerb.PUT);
        ra2 = new ResourcesAccess(2L, "ResourceAccess 2", "Microservice 2", "Resource 2", "Controller",
                HttpVerb.DELETE);
        ra3 = new ResourcesAccess(3L, "ResourceAccess 3", "Microservice 3", "Resource 3", "Controller", HttpVerb.GET);
        ras = new HashSet<>();
        ras.add(ra0);
        ras.add(ra1);
        ras.add(ra2);
        ras.add(ra3);
        Mockito.when(resourcesRepo.findOne(ra0.getId())).thenReturn(ra0);
        Mockito.when(resourcesRepo.findOne(ra1.getId())).thenReturn(ra1);
        Mockito.when(resourcesRepo.findOne(ra2.getId())).thenReturn(ra2);
        Mockito.when(resourcesRepo.findOne(ra3.getId())).thenReturn(ra3);

        discoveryClientMock = Mockito.mock(DiscoveryClient.class);

        roleServiceMock = Mockito.mock(IRoleService.class);
        roleAdmin = new Role("ADMIN", null);
        roleAdmin.setId(33L);
        Mockito.stub(roleServiceMock.retrieveInheritedRoles(Mockito.any(Role.class)))
                .toReturn(Sets.newHashSet(roleAdmin));
        Mockito.stub(roleServiceMock.retrieveRole("ADMIN")).toReturn(roleAdmin);

        tenantResolverMock = Mockito.mock(ITenantResolver.class);
        final Set<String> tenants = new HashSet<>();
        tenants.add("tenant1");
        Mockito.when(tenantResolverMock.getAllTenants()).thenReturn(tenants);

        SecurityUtils.mockActualRole("ADMIN");

        runtimeTenantResolverMock = Mockito.mock(IRuntimeTenantResolver.class);

        resourcesService = Mockito.spy(new ResourcesService(resourcesRepo, roleServiceMock));
    }

    @Purpose("Check that the collect resources functionnality is well done when no resources are collected")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testEmptyResourcesToCollect() throws EntityNotFoundException {
        resourcesRepo.deleteAll();
        Mockito.when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());
        final Page<ResourcesAccess> resultPage = resourcesService.retrieveRessources(new PageRequest(0, 20));
        Assert.assertNotNull(resultPage);
        Assert.assertEquals(resultPage.getNumberOfElements(), 0);
        Assert.assertTrue(resultPage.getContent().isEmpty());
    }

    @Test
    public void retrieveResourcesByMicroservice() throws EntityNotFoundException {

        final String ms = "rs-test";

        ResourcesAccess raTest1 = new ResourcesAccess("description", ms, "/resource/test/1", "Controller",
                HttpVerb.GET);
        roleAdmin.addPermission(raTest1);
        ResourcesAccess raTest2 = new ResourcesAccess("description", ms, "/resource/test/2", "Controller",
                HttpVerb.GET);
        roleAdmin.addPermission(raTest2);
        ResourcesAccess raTest3 = new ResourcesAccess("description", ms, "/resource/test/3", "Controller",
                HttpVerb.GET);
        roleAdmin.addPermission(raTest3);
        ResourcesAccess raTest4 = new ResourcesAccess("description", ms, "/resource/test/4", "Controller",
                HttpVerb.GET);
        roleAdmin.addPermission(raTest4);

        final Page<ResourcesAccess> page = resourcesService.retrieveMicroserviceRessources(ms, new PageRequest(0, 20));
        Assert.assertNotNull(page);
        Assert.assertNotNull(page.getContent());
        Assert.assertEquals(4, page.getContent().size());

    }

}
