/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.accessrights.client.IProjectUsersClient;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.registration.AccessRequestDto;
import fr.cnes.regards.modules.accessrights.service.projectuser.IProjectUserService;

/**
 * Test project endpoint client
 *
 * @author Marc Sordi
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class ProjectUsersFeignClientIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectUsersFeignClientIT.class);

    /**
     * Server
     */
    @Value("${server.address}")
    private String serverAddress;

    /**
     * Client to test
     */
    private IProjectUsersClient client;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    @Autowired
    private IProjectUserService projectUserService;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        client = FeignClientBuilder.build(new TokenClientProvider<>(IProjectUsersClient.class,
                "http://" + serverAddress + ":" + getPort(), feignSecurityManager));
        FeignSecurityManager.asSystem();
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Ignore
    @Test
    public void retrieveProjectUserListFromFeignClient() {
        final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = client.retrieveProjectUserList(0, 10);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveAccessRequestListFromFeignClient() {
        final ResponseEntity<PagedResources<Resource<ProjectUser>>> response = client.retrieveAccessRequestList(0, 10);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveProjectUserByEmailFromFeignClient() {
        final ResponseEntity<Resource<ProjectUser>> response = client.retrieveProjectUserByEmail("unkown@regards.de");
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Ignore
    @Test
    public void retrieveProjectUserFromFeignClient() {
        final ResponseEntity<Resource<ProjectUser>> response = client.retrieveProjectUser(1L);
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void removeProjectUserFromFeignClient() {
        final ResponseEntity<Void> response = client.removeProjectUser(new Long(150));
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.NOT_FOUND));
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     * @throws EntityException
     * @throws EntityInvalidException
     * @throws EntityAlreadyExistsException
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Ignore
    public void isAdminProjectUserFromFeignClient() throws EntityException {
        final AccessRequestDto accessRequest = new AccessRequestDto("test@c-s.fr", "pFirstName", "pLastName",
                DefaultRole.ADMIN.toString(), null, "pPassword", "pOriginUrl", "pRequestLink");

        projectUserService.createProjectUser(accessRequest);
        final ResponseEntity<Boolean> response = client.isAdmin("test@c-s.fr");
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
        Assert.assertTrue(response.getBody());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
