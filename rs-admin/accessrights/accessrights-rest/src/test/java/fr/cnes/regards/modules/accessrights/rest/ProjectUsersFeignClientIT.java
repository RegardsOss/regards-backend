/*
 * LICENSE_PLACEHOLDER
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

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
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
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
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
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void isAdminProjectUserFromFeignClient() throws EntityAlreadyExistsException {
        final AccessRequestDto accessRequest = new AccessRequestDto("regards-admin@c-s.fr", "pFirstName", "pLastName",
                                                                    DefaultRole.ADMIN.toString(), null, "pPassword",
                                                                    "pOriginUrl", "pRequestLink");

        projectUserService.createProjectUser(accessRequest);
        final ResponseEntity<Boolean> response = client.isAdmin("regards-admin@c-s.fr");
        Assert.assertTrue(response.getStatusCode().equals(HttpStatus.OK));
        Assert.assertTrue(response.getBody());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
