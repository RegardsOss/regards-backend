/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;

/**
 * @author Marc Sordi
 *
 */
@MultitenantTransactional
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
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPost(MicroserviceResourceController.TYPE_MAPPING, instanceToken, mapping, expectations,
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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performDefaultGet(MicroserviceResourceController.TYPE_MAPPING
                + MicroserviceResourceController.CONTROLLER_MAPPING, expectations,
                          "Error retrieving endpoints for microservice and controller", DEFAULT_MICROSERVICE,
                          DEFAULT_CONTROLLER);

        // Check that no endpoint is returned for an unknown controller name
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isEmpty());
        performDefaultGet(MicroserviceResourceController.TYPE_MAPPING
                + MicroserviceResourceController.CONTROLLER_MAPPING, expectations,
                          "Error retrieving endpoints for microservice and controller", DEFAULT_MICROSERVICE,
                          "unknown-controller");

        // Check that no endpoint is returned for an unknown microservice name
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isEmpty());
        performDefaultGet(MicroserviceResourceController.TYPE_MAPPING
                + MicroserviceResourceController.CONTROLLER_MAPPING, expectations,
                          "Error retrieving endpoints for microservice and controller", "unkonwon-microservice",
                          DEFAULT_CONTROLLER);
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
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performDefaultGet(MicroserviceResourceController.TYPE_MAPPING
                + MicroserviceResourceController.CONTROLLERS_MAPPING, expectations,
                          "Error retrieving endpoints controllers names for microservice", DEFAULT_MICROSERVICE);

        // Check that no controllers are returned for an unknown controller name
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isEmpty());
        performDefaultGet(MicroserviceResourceController.TYPE_MAPPING
                + MicroserviceResourceController.CONTROLLERS_MAPPING, expectations,
                          "Error retrieving endpoints controllers names for microservice", "unkonwon-microservice");
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
