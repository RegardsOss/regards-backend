/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.stubs.ResourcesAccessRepositoryStub;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
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
     * Stub for JPA Repository
     */
    private final IResourcesAccessRepository resourcesRepo = new ResourcesAccessRepositoryStub();

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

        discoveryClientMock = Mockito.mock(DiscoveryClient.class);

        roleServiceMock = Mockito.mock(IRoleService.class);
        final Role roleAdmin = new Role("ADMIN", null);
        roleAdmin.setId(33L);
        Mockito.stub(roleServiceMock.retrieveInheritedRoles(Mockito.any(Role.class)))
                .toReturn(Sets.newHashSet(roleAdmin));
        Mockito.stub(roleServiceMock.retrieveRole("ADMIN")).toReturn(roleAdmin);

        tenantResolverMock = Mockito.mock(ITenantResolver.class);
        final Set<String> tenants = new HashSet<>();
        tenants.add("tenant1");
        Mockito.when(tenantResolverMock.getAllTenants()).thenReturn(tenants);

        JWTService jwtService = new JWTService();
        jwtService.setSecret("123456789");

        jwtService = Mockito.spy(jwtService);
        Mockito.stub(jwtService.getActualRole()).toReturn("ADMIN");

        resourcesService = Mockito.spy(new ResourcesService("rs-test", discoveryClientMock, resourcesRepo,
                roleServiceMock, jwtService, tenantResolverMock, Mockito.mock(IPublisher.class)));

    }

    @Purpose("Check that the collect resources functionnality is well done when no resources are collected")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testEmptyResourcesToCollect() throws EntityNotFoundException {
        resourcesRepo.deleteAll();
        Mockito.when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());
        resourcesService.init();
        final Page<ResourcesAccess> resultPage = resourcesService.retrieveRessources(new PageRequest(0, 20));
        Assert.assertNotNull(resultPage);
        Assert.assertEquals(resultPage.getNumberOfElements(), 0);
        Assert.assertTrue(resultPage.getContent().isEmpty());
    }

    /**
     * Check that the collect resources functionnality is well done for remote services resources.
     *
     * @throws EntityNotFoundException
     *             when no role with passed name could be found
     */
    @Purpose("Check that the collect resources functionnality is well done for remote services resources")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testRemoteResourcesToCollect() throws EntityNotFoundException {

        resourcesRepo.deleteAll();

        final List<ResourceMapping> resources = new ArrayList<>();
        final Map<String, Object> attributs = new HashMap<>();
        attributs.put("name", "/test/premier");
        attributs.put("description", "premier test");
        attributs.put("role", DefaultRole.ADMIN);
        ResourceAccess resourceAccess = AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
        resources.add(new ResourceMapping(resourceAccess, "/test/premier", RequestMethod.GET));
        attributs.put("name", "/test/second");
        attributs.put("description", "second test");
        resourceAccess = AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
        resources.add(new ResourceMapping(resourceAccess, "/test/second", RequestMethod.POST));
        attributs.put("name", "/test/third");
        attributs.put("description", "third test");
        resourceAccess = AnnotationUtils.synthesizeAnnotation(attributs, ResourceAccess.class, null);
        resources.add(new ResourceMapping(resourceAccess, "/test/third", RequestMethod.DELETE));

        final List<String> services = new ArrayList<>();
        services.add("test-service");

        Mockito.when(discoveryClientMock.getServices()).thenReturn(services);

        final RoleFactory factory = new RoleFactory();
        Mockito.when(roleServiceMock.retrieveRole(DefaultRole.ADMIN.toString()))
                .thenReturn(factory.withId(1L).createAdmin());

        Mockito.doReturn(resources).when(resourcesService).getRemoteResources(Mockito.anyString());

        resourcesService.init();
        Assert.assertTrue(resourcesRepo.count() == 3);

        for (final ResourcesAccess resource : resourcesService.retrieveRessources(new PageRequest(0, 20))) {
            Assert.assertTrue(!resource.getRoles().isEmpty());
            final Optional<Role> found = resource.getRoles().stream()
                    .filter(r -> r.getName().equals(DefaultRole.ADMIN.toString())).findFirst();
            Assert.assertTrue(found.isPresent());
        }

    }

    @Test
    public void retrieveResourcesByMicroservice() {

        final String ms = "rs-test";
        final Role roleAdmin = new Role("ADMIN", null);

        ResourcesAccess ra = new ResourcesAccess("description", ms, "/resource/test/1", HttpVerb.GET);
        ra.addRole(roleAdmin);
        resourcesRepo.save(ra);
        ra = new ResourcesAccess("description", ms, "/resource/test/2", HttpVerb.GET);
        ra.addRole(roleAdmin);
        resourcesRepo.save(ra);
        ra = new ResourcesAccess("description", ms, "/resource/test/3", HttpVerb.GET);
        ra.addRole(roleAdmin);
        resourcesRepo.save(ra);
        ra = new ResourcesAccess("description", ms, "/resource/test/4", HttpVerb.GET);
        resourcesRepo.save(ra);
        final Page<ResourcesAccess> page = resourcesService.retrieveMicroserviceRessources(ms, new PageRequest(0, 20));
        Assert.assertNotNull(page);
        Assert.assertNotNull(page.getContent());
        Assert.assertEquals(3, page.getContent().size());

    }

}
