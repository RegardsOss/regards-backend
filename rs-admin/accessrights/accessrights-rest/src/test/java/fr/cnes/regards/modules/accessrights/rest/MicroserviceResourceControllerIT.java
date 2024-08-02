/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.security.annotation.ResourceAccessAdapter;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marc Sordi
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class MicroserviceResourceControllerIT extends AbstractRegardsTransactionalIT {

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

    @MockBean
    private QuotaHelperService quotaHelperService;

    /**
     * Initialize all datas for this unit tests
     */
    @Before
    public void initResources() {

        JWTService service = new JWTService();
        service.setSecret("!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!");
        instanceToken = service.generateToken(getDefaultTenant(),
                                              getDefaultUserEmail(),
                                              DefaultRole.INSTANCE_ADMIN.toString());

        ResourcesAccess resource = new ResourcesAccess("description",
                                                       DEFAULT_MICROSERVICE,
                                                       CONFIGURED_ENDPOINT_URL,
                                                       DEFAULT_CONTROLLER,
                                                       RequestMethod.GET,
                                                       DefaultRole.ADMIN);
        resource = resourcesAccessRepository.save(resource);
    }

    /**
     * Check first registration of a microservice endpoints
     */
    @Test
    @Purpose("Check first registration of a microservice endpoints")
    public void registerMicroserviceEndpointsTest() {
        List<ResourceMapping> mapping = new ArrayList<>();
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.PUBLIC),
                                        "/endpoint/test",
                                        DEFAULT_CONTROLLER,
                                        RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.REGISTERED_USER),
                                        "/endpoint/test2",
                                        DEFAULT_CONTROLLER,
                                        RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.INSTANCE_ADMIN),
                                        "/endpoint/test3",
                                        DEFAULT_CONTROLLER,
                                        RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.PUBLIC),
                                        CONFIGURED_ENDPOINT_URL,
                                        DEFAULT_CONTROLLER,
                                        RequestMethod.GET));
        performPost(MicroserviceResourceController.TYPE_MAPPING,
                    instanceToken,
                    mapping,
                    customizer().expectStatusOk(),
                    "Error during registring endpoints",
                    DEFAULT_MICROSERVICE);

    }

    /**
     * Check that the microservice allow to retrieve all resource endpoints configurations for a given microservice name
     * and a given controller name
     */
    @Test
    @Purpose("Check that the microservice allow to retrieve resource endpoints for a microservice and a controller")
    public void retrieveMicroserviceControllerEndpointsTest() {

        // Check that the microservice return the initialized endpoit.
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLER_MAPPING,
                   instanceToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_ROOT).expectIsNotEmpty(JSON_PATH_ROOT),
                   "Error retrieving endpoints for microservice and controller",
                   DEFAULT_MICROSERVICE,
                   DEFAULT_CONTROLLER);
    }

    @Test
    public void retrieveNothingForUnknownController() {
        // Check that no endpoint is returned for an unknown controller name
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLER_MAPPING,
                   instanceToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_ROOT).expectIsEmpty(JSON_PATH_ROOT),
                   "Error retrieving endpoints for microservice and controller",
                   DEFAULT_MICROSERVICE,
                   "unknown-controller");

    }

    @Test
    public void retrieveNothingForUnknownMicroservice() {
        // Check that no endpoint is returned for an unknown microservice name
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLER_MAPPING,
                   instanceToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_ROOT).expectIsEmpty(JSON_PATH_ROOT),
                   "Error retrieving endpoints for microservice and controller",
                   "unknown-microservice",
                   DEFAULT_CONTROLLER);
    }

    /**
     * Check that the microservice allow to retrieve all resource endpoints configurations for a given microservice name
     * and a given controller name
     */
    @Test
    @Purpose("Check that the microservice allow to retrieve a microservice resources crontrollers name")
    public void retrieveMicroserviceControllersTest() {

        // Check that the microservice return is controllers names from is initialized resources.
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLERS_MAPPING,
                   instanceToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_ROOT).expectIsNotEmpty(JSON_PATH_ROOT),
                   "Error retrieving endpoints controllers names for microservice",
                   DEFAULT_MICROSERVICE);

        // Check that no controllers are returned for an unknown controller name
        performGet(MicroserviceResourceController.TYPE_MAPPING + MicroserviceResourceController.CONTROLLERS_MAPPING,
                   instanceToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_ROOT).expectIsEmpty(JSON_PATH_ROOT),
                   "Error retrieving endpoints controllers names for microservice",
                   "unkonwon-microservice");
    }

}
