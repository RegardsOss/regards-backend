/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.microservices.administration;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.RegardsSpringRunner;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.domain.projects.RoleFactory;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import fr.cnes.regards.modules.dam.client.dataaccess.IAccessGroupClient;
import fr.cnes.regards.modules.storage.client.IStorageSettingClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * Class LocalAuthoritiesProviderTest
 *
 * Test for administration local AuthoritiesProvider
 * @author Sébastien Binda
 */
@RunWith(RegardsSpringRunner.class)
@SpringBootTest
@EnableAutoConfiguration
@ContextConfiguration(classes = { AuthoritiesTestConfiguration.class })
@MultitenantTransactional
@ActiveProfiles("test")
public class LocalAuthoritiesProviderTest {

    /**
     * Current microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    /**
     * Runtime tenant resolver
     */
    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Authorities provider to test
     */
    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @MockBean
    private IAccessGroupClient accessGroupClient;

    @MockBean
    private IExternalAuthenticationClient externalAuthenticationClient;

    @MockBean
    private IStorageSettingClient storageSettingClient;

    @BeforeTransaction
    public void beforeTransaction() {
        runtimeTenantResolver.forceTenant("test-project");
    }

    @Before
    public void setUp() {
        resourcesAccessRepository.deleteAll();
        roleRepository.deleteAll();

        List<String> addresses = new ArrayList<>();
        addresses.add("127.0.0.1");
        addresses.add("127.0.0.2");
        addresses.add("127.0.0.3");
        RoleFactory roleFactory = new RoleFactory();

        roleFactory.withId(0L).withAuthorizedAddresses(addresses).withDefault(false).withNative(true);

        Role publicRole = roleRepository.findOneByName(DefaultRole.PUBLIC.toString())
                .orElseGet(() -> roleRepository.save(roleFactory.createPublic()));

        roleFactory.withParentRole(publicRole);

        roleRepository.findOneByName(AuthoritiesTestConfiguration.ROLE_NAME)
                .ifPresent(role -> roleRepository.deleteById(role.getId()));
        roleRepository.save(roleFactory.withName(AuthoritiesTestConfiguration.ROLE_NAME).create());

        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.GET, DefaultRole.ADMIN));
        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.PUT, DefaultRole.ADMIN));
        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.POST, DefaultRole.ADMIN));
        resourcesAccessRepository.save(new ResourcesAccess(0L, "description", microserviceName, "/resource",
                "Controller", RequestMethod.DELETE, DefaultRole.ADMIN));
    }

    @Test
    public void test() {
        // Test initialization
    }
}
