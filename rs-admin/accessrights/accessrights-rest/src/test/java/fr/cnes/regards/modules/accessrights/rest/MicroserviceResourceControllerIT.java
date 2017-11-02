/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class MicroserviceResourceControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MicroserviceResourceControllerIT.class);

    /**
     * Default endpoint url configured for this test
     */
    private static final String CONFIGURED_ENDPOINT_URL = "/configured/endpoint";

    /**
     * Default microservice used for this test
     */
    private static final String DEFAULT_MICROSERVICE = "rs-test";

    /**
     * Default controller name used for resourceAccess tests.
     */
    private static final String DEFAULT_CONTROLLER = "testController";

    /**
     * Security token to access ADMIN_INSTANCE endpoints
     */
    private String instanceToken;

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    /**
     *
     * Initialize all datas for this unit tests
     *
     * @throws EntityNotFoundException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Before
    public void initResources() throws EntityNotFoundException {

        final JWTService service = new JWTService();
        service.setSecret("123456789");
        instanceToken = service.generateToken(DEFAULT_TENANT, DEFAULT_USER_EMAIL,
                                              DefaultRole.INSTANCE_ADMIN.toString());

        ResourcesAccess resource = new ResourcesAccess("description", DEFAULT_MICROSERVICE, CONFIGURED_ENDPOINT_URL,
                DEFAULT_CONTROLLER, RequestMethod.GET, DefaultRole.ADMIN);
        resource = resourcesAccessRepository.save(resource);
    }

    /**
     *
     * Check first registration of a microservice endpoints
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check first registration of a microservice endpoints")
    public void registerMicroserviceEndpointsTest() {
        final List<ResourceMapping> mapping = new ArrayList<>();
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.PUBLIC),
                "/endpoint/test", DEFAULT_CONTROLLER, RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.REGISTERED_USER),
                "/endpoint/test2", DEFAULT_CONTROLLER, RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.INSTANCE_ADMIN),
                "/endpoint/test3", DEFAULT_CONTROLLER, RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.PUBLIC),
                CONFIGURED_ENDPOINT_URL, DEFAULT_CONTROLLER, RequestMethod.GET));
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performPost(MicroserviceResourceController.TYPE_MAPPING, instanceToken, mapping, requestBuilderCustomizer,
                    "Error during registring endpoints", DEFAULT_MICROSERVICE);

    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations for a given microservice name
     * and a given controller name
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the microservice allow to retrieve resource endpoints for a microservice and a controller")
    public void retrieveMicroserviceControllerEndpointsTest() {

        // Check that the microservice return the initialized endpoit.
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLER_MAPPING,
                   instanceToken, requestBuilderCustomizer, "Error retrieving endpoints for microservice and controller",
                   DEFAULT_MICROSERVICE, DEFAULT_CONTROLLER);

        // Check that no endpoint is returned for an unknown controller name
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isEmpty());
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLER_MAPPING,
                   instanceToken, requestBuilderCustomizer, "Error retrieving endpoints for microservice and controller",
                   DEFAULT_MICROSERVICE, "unknown-controller");

        // Check that no endpoint is returned for an unknown microservice name
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isEmpty());
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLER_MAPPING,
                   instanceToken, requestBuilderCustomizer, "Error retrieving endpoints for microservice and controller",
                   "unkonwon-microservice", DEFAULT_CONTROLLER);
    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations for a given microservice name
     * and a given controller name
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the microservice allow to retrieve a microservice resources crontrollers name")
    public void retrieveMicroserviceControllersTest() {

        // Check that the microservice return is controllers names from is initialized resources.
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLERS_MAPPING,
                   instanceToken, requestBuilderCustomizer, "Error retrieving endpoints controllers names for microservice",
                   DEFAULT_MICROSERVICE);

        // Check that no controllers are returned for an unknown controller name
        requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isEmpty());
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLERS_MAPPING,
                   instanceToken, requestBuilderCustomizer, "Error retrieving endpoints controllers names for microservice",
                   "unkonwon-microservice");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
