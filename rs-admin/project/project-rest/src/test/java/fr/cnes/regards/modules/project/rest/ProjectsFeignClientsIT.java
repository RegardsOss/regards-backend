/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.rest;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.support.ResponseEntityDecoder;
import org.springframework.cloud.netflix.feign.support.SpringMvcContract;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMethod;

import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import fr.cnes.regards.client.core.TokenClientProvider;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 *
 * Class ProjectsFeignClientsIT
 *
 * Project feign clients.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@EnableFeignClients(clients = { IProjectsClient.class })
public class ProjectsFeignClientsIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectsFeignClientsIT.class);

    @Value("${server.address}")
    private String serverAddress;

    /**
     *
     * Check that the projects Feign Client handle the pagination parameters.
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void retrieveAllProjectsByPageFromFeignClient() {
        try {
            authService.setAuthorities(DEFAULT_TENANT, "/projects", RequestMethod.GET,
                                       DefaultRole.INSTANCE_ADMIN.toString());
            jwtService.injectToken(DEFAULT_TENANT, DefaultRole.INSTANCE_ADMIN.toString(), "");
            final IProjectsClient projectsClient = HystrixFeign.builder().contract(new SpringMvcContract())
                    .encoder(new GsonEncoder()).decoder(new ResponseEntityDecoder(new GsonDecoder()))
                    .target(new TokenClientProvider<>(IProjectsClient.class,
                            "http://" + serverAddress + ":" + getPort()));
            final ResponseEntity<PagedResources<Resource<Project>>> projects = projectsClient.retrieveProjectList(0,
                                                                                                                  10);
            Assert.assertTrue(projects.getStatusCode().equals(HttpStatus.OK));
        } catch (final Exception e) {
            LOG.error(e.getMessage(), e);
            Assert.fail(e.getMessage());
        }
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
