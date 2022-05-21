/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.accessrights.service;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.resources.ResourcesService;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Class ResourcesServiceTest
 * <p>
 * Test for resources management
 *
 * @author Sébastien Binda
 * @author Christophe Mertz
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

    private IAuthenticationResolver authResolver;

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
     * Initialization of mocks and stubs
     *
     * @throws EntityNotFoundException test error
     */
    @Before
    public void init() throws EntityNotFoundException {

        resourcesRepo = Mockito.mock(IResourcesAccessRepository.class);
        ra0 = new ResourcesAccess(0L,
                                  "ResourceAccess 0",
                                  "Microservice 0",
                                  "Resource 0",
                                  "Controller",
                                  RequestMethod.GET,
                                  DefaultRole.ADMIN);
        ra1 = new ResourcesAccess(1L,
                                  "ResourceAccess 1",
                                  "Microservice 1",
                                  "Resource 1",
                                  "Controller",
                                  RequestMethod.PUT,
                                  DefaultRole.ADMIN);
        ra2 = new ResourcesAccess(2L,
                                  "ResourceAccess 2",
                                  "Microservice 2",
                                  "Resource 2",
                                  "Controller",
                                  RequestMethod.DELETE,
                                  DefaultRole.ADMIN);
        ra3 = new ResourcesAccess(3L,
                                  "ResourceAccess 3",
                                  "Microservice 3",
                                  "Resource 3",
                                  "Controller",
                                  RequestMethod.GET,
                                  DefaultRole.ADMIN);
        ras = new HashSet<>();
        ras.add(ra0);
        ras.add(ra1);
        ras.add(ra2);
        ras.add(ra3);
        Mockito.when(resourcesRepo.findById(ra0.getId())).thenReturn(Optional.of(ra0));
        Mockito.when(resourcesRepo.findById(ra1.getId())).thenReturn(Optional.of(ra1));
        Mockito.when(resourcesRepo.findById(ra2.getId())).thenReturn(Optional.of(ra2));
        Mockito.when(resourcesRepo.findById(ra3.getId())).thenReturn(Optional.of(ra3));

        discoveryClientMock = Mockito.mock(DiscoveryClient.class);

        roleServiceMock = Mockito.mock(IRoleService.class);
        roleAdmin = new Role("ADMIN", null);
        roleAdmin.setId(33L);
        Mockito.when(roleServiceMock.retrieveInheritedRoles(Mockito.any(Role.class)))
               .thenReturn(Sets.newHashSet(roleAdmin));
        Mockito.when(roleServiceMock.retrieveRole("ADMIN")).thenReturn(roleAdmin);

        tenantResolverMock = Mockito.mock(ITenantResolver.class);
        final Set<String> tenants = new HashSet<>();
        tenants.add("tenant1");
        Mockito.when(tenantResolverMock.getAllTenants()).thenReturn(tenants);

        authResolver = Mockito.mock(IAuthenticationResolver.class);
        Mockito.when(authResolver.getRole()).thenReturn("ADMIN");

        resourcesService = Mockito.spy(new ResourcesService(resourcesRepo, roleServiceMock, authResolver));
    }

    @Purpose("Check that the collect resources functionnality is well done when no resources are collected")
    @Requirement("REGARDS_DSL_ADM_ADM_240")
    @Test
    public void testEmptyResourcesToCollect() throws ModuleException {
        resourcesRepo.deleteAll();
        Mockito.when(discoveryClientMock.getServices()).thenReturn(new ArrayList<>());
        final Page<ResourcesAccess> resultPage = resourcesService.retrieveRessources(null, PageRequest.of(0, 20));
        Assert.assertNotNull(resultPage);
        Assert.assertEquals(resultPage.getNumberOfElements(), 0);
        Assert.assertTrue(resultPage.getContent().isEmpty());
    }

    @Test
    public void retrieveResourcesByMicroservice() throws ModuleException {

        final String ms = "rs-test";

        ResourcesAccess raTest1 = new ResourcesAccess("description",
                                                      ms,
                                                      "/resource/test/1",
                                                      "Controller",
                                                      RequestMethod.GET,
                                                      DefaultRole.ADMIN);
        roleAdmin.addPermission(raTest1);
        ResourcesAccess raTest2 = new ResourcesAccess("description",
                                                      ms,
                                                      "/resource/test/2",
                                                      "Controller",
                                                      RequestMethod.GET,
                                                      DefaultRole.ADMIN);
        roleAdmin.addPermission(raTest2);
        ResourcesAccess raTest3 = new ResourcesAccess("description",
                                                      ms,
                                                      "/resource/test/3",
                                                      "Controller",
                                                      RequestMethod.GET,
                                                      DefaultRole.ADMIN);
        roleAdmin.addPermission(raTest3);
        ResourcesAccess raTest4 = new ResourcesAccess("description",
                                                      ms,
                                                      "/resource/test/4",
                                                      "Controller",
                                                      RequestMethod.GET,
                                                      DefaultRole.ADMIN);
        roleAdmin.addPermission(raTest4);

        final Page<ResourcesAccess> page = resourcesService.retrieveRessources(ms, PageRequest.of(0, 20));
        Assert.assertNotNull(page);
        Assert.assertNotNull(page.getContent());
        Assert.assertEquals(4, page.getContent().size());

    }

}
