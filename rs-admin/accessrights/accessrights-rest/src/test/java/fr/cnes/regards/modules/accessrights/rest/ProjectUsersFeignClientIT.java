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
package fr.cnes.regards.modules.accessrights.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;

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

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        client = FeignClientBuilder.build(
                                          new TokenClientProvider<>(IProjectUsersClient.class,
                                                  "http://" + serverAddress + ":" + getPort(), feignSecurityManager),
                                          gson);
        FeignSecurityManager.asSystem();
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *

     */
    @Ignore
    @Test
    public void retrieveProjectUserListFromFeignClient() {
        final ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response = client.retrieveProjectUserList(null, null,0, 10);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *

     */
    @Test
    public void retrieveAccessRequestListFromFeignClient() {
        final ResponseEntity<PagedModel<EntityModel<ProjectUser>>> response = client.retrieveAccessRequestList(0, 10);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *

     */
    @Test
    public void retrieveProjectUserByEmailFromFeignClient() {
        final ResponseEntity<EntityModel<ProjectUser>> response = client
                .retrieveProjectUserByEmail("unkown@regards.de");
        Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *

     */
    @Ignore
    @Test
    public void retrieveProjectUserFromFeignClient() {
        final ResponseEntity<EntityModel<ProjectUser>> response = client.retrieveProjectUser(1L);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     *

     */
    @Test
    public void removeProjectUserFromFeignClient() {
        final ResponseEntity<Void> response = client.removeProjectUser(150L);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.NOT_FOUND);
    }

    /**
     *
     * Check that the accounts Feign Client can retrieve all accounts.
     * @throws EntityException
     * @throws EntityInvalidException
     * @throws EntityAlreadyExistsException
     *

     */
    @Test
    @Ignore
    public void isAdminProjectUserFromFeignClient() throws EntityException {
        final AccessRequestDto accessRequest = new AccessRequestDto("test@c-s.fr", "pFirstName", "pLastName",
                DefaultRole.ADMIN.toString(), null, "pPassword", "pOriginUrl", "pRequestLink");

        projectUserService.createProjectUser(accessRequest);
        final ResponseEntity<Boolean> response = client.isAdmin("test@c-s.fr");
        Assert.assertEquals(response.getStatusCode(), HttpStatus.OK);
        Assert.assertTrue(response.getBody());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
