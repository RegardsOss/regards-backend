/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.stubs.ResourcesAccessRepositoryStub;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
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
     * Mock to manage method security
     */
    private MethodAuthorizationService methodAuthServiceMock;

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
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void init() {

        discoveryClientMock = Mockito.mock(DiscoveryClient.class);

        roleServiceMock = Mockito.mock(IRoleService.class);

        methodAuthServiceMock = Mockito.mock(MethodAuthorizationService.class);

        tenantResolverMock = Mockito.mock(ITenantResolver.class);
        final Set<String> tenants = new HashSet<>();
        tenants.add("tenant1");
        Mockito.when(tenantResolverMock.getAllTenants()).thenReturn(tenants);

        final JWTService jwtService = new JWTService();
        jwtService.setSecret("123456789");

        resourcesService = Mockito.spy(new ResourcesService("rs-test", discoveryClientMock, resourcesRepo,
                roleServiceMock, methodAuthServiceMock, jwtService, tenantResolverMock));

    }

    @Purpose("Check that the collect resources functionnality is well done when no resources are collected")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testEmptyResourcesToCollect() {
        resourcesRepo.deleteAll();
        Mockito.when(methodAuthServiceMock.getResources()).thenReturn(new ArrayList<>());
        Mockito.when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());
        resourcesService.init();
        Assert.assertTrue(resourcesService.retrieveRessources().isEmpty());
    }

    @Purpose("Check that the collect resources functionnality is well done for local administration service resources")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testLocalResourcesToCollect() {

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
        Mockito.when(methodAuthServiceMock.getResources()).thenReturn(resources);
        Mockito.when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());

        final RoleFactory factory = new RoleFactory();
        Mockito.when(roleServiceMock.retrieveRole(DefaultRole.ADMIN.toString()))
                .thenReturn(factory.withId(1L).createAdmin());

        Assert.assertTrue(resourcesService.collectResources().size() == 3);
        Assert.assertTrue(resourcesRepo.count() == 3);

        for (final ResourcesAccess resource : resourcesService.retrieveRessources()) {
            Assert.assertTrue(!resource.getRoles().isEmpty());
            Assert.assertTrue(resource.getRoles().get(0).getName().equals(DefaultRole.ADMIN.toString()));
        }

    }

    @Purpose("Check that the collect resources functionnality is well done for remote services resources")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testRemoteResourcesToCollect() {

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

        Mockito.when(methodAuthServiceMock.getResources()).thenReturn(new ArrayList<>());
        final List<String> services = new ArrayList<>();
        services.add("test-service");

        Mockito.when(discoveryClientMock.getServices()).thenReturn(services);

        final RoleFactory factory = new RoleFactory();
        Mockito.when(roleServiceMock.retrieveRole(DefaultRole.ADMIN.toString()))
                .thenReturn(factory.withId(1L).createAdmin());

        Mockito.doReturn(resources).when(resourcesService).getRemoteResources(Mockito.anyString());

        Assert.assertTrue(resourcesService.collectResources().size() == 3);
        Assert.assertTrue(resourcesRepo.count() == 3);

        for (final ResourcesAccess resource : resourcesService.retrieveRessources()) {
            Assert.assertTrue(!resource.getRoles().isEmpty());
            Assert.assertTrue(resource.getRoles().get(0).getName().equals(DefaultRole.ADMIN.toString()));
        }

    }

}
