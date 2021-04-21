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
package fr.cnes.regards.modules.project.rest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;

import com.google.gson.Gson;

import fr.cnes.regards.framework.feign.FeignClientBuilder;
import fr.cnes.regards.framework.feign.TokenClientProvider;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.test.integration.AbstractRegardsWebIT;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

/**
 * Class ProjectsFeignClientsIT
 *
 * Project feign clients.
 * @author Sébastien Binda
 */
@ContextConfiguration(classes = { LicenseConfiguration.class })
public class ProjectsFeignClientsIT extends AbstractRegardsWebIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ProjectsFeignClientsIT.class);

    @Value("${server.address}")
    private String serverAddress;

    /**
     * Feign security manager
     */
    @Autowired
    private FeignSecurityManager feignSecurityManager;

    /**
     * Client to test
     */
    private IProjectsClient client;

    @Autowired
    private Gson gson;

    @Before
    public void init() {
        client = FeignClientBuilder.build(
                                          new TokenClientProvider<>(IProjectsClient.class,
                                                  "http://" + serverAddress + ":" + getPort(), feignSecurityManager),
                                          gson);
        FeignSecurityManager.asSystem();
    }

    /**
     * Check that the projects Feign Client handle the pagination parameters.
     */
    @Test
    public void retrieveAllProjectsByPageFromFeignClient() {
        final ResponseEntity<PagedModel<EntityModel<Project>>> projects = client.retrieveProjectList(0, 10);
        Assert.assertEquals(projects.getStatusCode(), HttpStatus.OK);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
