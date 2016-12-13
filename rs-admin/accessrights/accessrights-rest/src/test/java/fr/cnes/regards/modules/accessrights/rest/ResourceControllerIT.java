/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
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
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.accessrights.dao.projects.IProjectUserRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.domain.HttpVerb;
import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.role.IRoleService;

/**
 *
 * Class ResourceControllerIT
 *
 * Test class to check access to {@link ResourcesAccess} entities. Those entities are used to configure the authroized
 * access to microservices endpoints.
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@MultitenantTransactional
public class ResourceControllerIT extends AbstractRegardsTransactionalIT {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(ResourceControllerIT.class);

    /**
     * Default endpoint url configured for this test
     */
    private static final String CONFIGURED_ENDPOINT_URL = "/configured/endpoint";

    /**
     * Default microservice used for this test
     */
    private static final String DEFAULT_MICROSERVICE = "rs-test";

    /**
     * JPA Respository used to initialize datas and check results
     */
    @Autowired
    private IResourcesAccessRepository repository;

    @Autowired
    private IProjectUserRepository projectUserRepository;

    /**
     * Service to manage Role entities
     */
    @Autowired
    private IRoleService roleService;

    /**
     * Security token to access ADMIN_INSTANCE endpoints
     */
    private String instanceToken;

    /**
     * Security token to access ADMIN endpoints
     */
    private String publicToken;

    private ResourcesAccess testResource;

    private ProjectUser testUser;

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
        publicToken = service.generateToken(DEFAULT_TENANT, DEFAULT_USER_EMAIL, DefaultRole.PUBLIC.toString());

        final ResourcesAccess resource = new ResourcesAccess("description", DEFAULT_MICROSERVICE,
                CONFIGURED_ENDPOINT_URL, HttpVerb.GET);
        final Role adminRole = roleService.retrieveRole(DefaultRole.ADMIN.toString());
        resource.addRole(adminRole);

        testUser = projectUserRepository
                .save(new ProjectUser(DEFAULT_USER_EMAIL, adminRole, new ArrayList<>(), new ArrayList<>()));
        testResource = repository.save(resource);
    }

    /**
     *
     * Check first registration of a microservice endpoints
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check first registration of a microservice endpoints")
    public void initialMicroserviceEndpointsRegistration() {

        final List<ResourceMapping> mapping = new ArrayList<>();
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.PUBLIC),
                "/endpoint/test", RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.REGISTERED_USER),
                "/endpoint/test2", RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.INSTANCE_ADMIN),
                "/endpoint/test3", RequestMethod.GET));
        mapping.add(new ResourceMapping(ResourceAccessAdapter.createResourceAccess("test", DefaultRole.PUBLIC),
                CONFIGURED_ENDPOINT_URL, RequestMethod.GET));
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performPost(ResourcesController.REQUEST_MAPPING_ROOT + "/register/microservices/{microservice}", instanceToken,
                    mapping, expectations, "Error during registring endpoints", DEFAULT_MICROSERVICE);

        final List<ResourcesAccess> resources = repository.findByMicroservice(DEFAULT_MICROSERVICE);

        // Check that the first endpoint is accessible by all default roles
        Optional<ResourcesAccess> resource = resources.stream().filter(r -> r.getResource().equals("/endpoint/test"))
                .findFirst();
        final List<Role> iop = resource.get().getRoles();
        for (final Role plop : iop) {
            Assert.assertNotNull(plop);
        }
        Assert.assertTrue(resource.get().getRoles().size() == (DefaultRole.values().length));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.PUBLIC.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.REGISTERED_USER.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.ADMIN.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.PROJECT_ADMIN.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.INSTANCE_ADMIN.toString())));

        // Check that the second endpoint is accessible by all default roles except public
        resource = resources.stream().filter(r -> r.getResource().equals("/endpoint/test2")).findFirst();
        Assert.assertTrue(resource.get().getRoles().size() == ((DefaultRole.values().length - 1)));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.REGISTERED_USER.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.ADMIN.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.PROJECT_ADMIN.toString())));
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.INSTANCE_ADMIN.toString())));

        // Check that the first endpoint is only accessible by the INSTANCE_ADMIN Role
        resource = resources.stream().filter(r -> r.getResource().equals("/endpoint/test3")).findFirst();
        Assert.assertTrue(resource.get().getRoles().size() == 1);
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.INSTANCE_ADMIN.toString())));

        // Check that the already configured endpoint do not overide configuration with default endpoint role and keep
        // already configured authorized roles (see initResources method). So only ADMIN is
        // authorized
        resource = resources.stream().filter(r -> r.getResource().equals(CONFIGURED_ENDPOINT_URL.toString()))
                .findFirst();
        Assert.assertTrue(resource.get().getRoles().size() == 1);
        Assert.assertTrue(resource.get().getRoles().stream()
                .anyMatch(r -> r.getName().equals(DefaultRole.ADMIN.toString())));

    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the microservice allow to retrieve all resource endpoints configurations")
    public void retrieveResources() {
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        performGet(ResourcesController.REQUEST_MAPPING_ROOT, publicToken, expectations, "Error retrieving endpoints");
    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the microservice allow to retrieve one resource endpoint configurations")
    public void retrieveResource() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet(ResourcesController.REQUEST_MAPPING_ROOT + "/" + testResource.getId(), publicToken, expectations,
                   "Error retrieving endpoints");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_230")
    @Purpose("Check that the system allows to retrieve a user's permissions.")
    public void getUserPermissions() {
        final List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ResourcesController.REQUEST_MAPPING_ROOT + "/users/{user_email}", expectations,
                          "Error retrieving resourcesAccess for user.", testUser.getEmail());

        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isNotFound());
        performDefaultGet(ResourcesController.REQUEST_MAPPING_ROOT + "/users/{user_email}", expectations,
                          "The user does not exists. There should be an error 404", "wrongEmail");
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
