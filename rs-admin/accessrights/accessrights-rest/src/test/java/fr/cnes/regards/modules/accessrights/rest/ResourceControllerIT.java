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
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

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
     * Default controller name used for resourceAccess tests.
     */
    private static final String DEFAULT_CONTROLLER = "testController";

    @Autowired
    private IResourcesAccessRepository resourcesAccessRepository;

    @Autowired
    private IRoleRepository roleRepository;

    /**
     * Security token for PUBLIC
     */
    private String publicToken;

    /**
     * Security token for PROJECT_ADMIN
     */
    private String projectAdminToken;

    /**
     * Security token for INSTANCE_ADMIN
     */
    private String instanceAdminToken;

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
        publicToken = service.generateToken(DEFAULT_TENANT, DEFAULT_USER_EMAIL, DefaultRole.PUBLIC.toString());
        projectAdminToken = service.generateToken(DEFAULT_TENANT, DEFAULT_USER_EMAIL,
                                                  DefaultRole.PROJECT_ADMIN.toString());
        instanceAdminToken = service.generateToken(DEFAULT_TENANT, DEFAULT_USER_EMAIL,
                                                   DefaultRole.INSTANCE_ADMIN.toString());
    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations as PUBLIC
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the microservice allows to retrieve all resource endpoints configurations")
    public void getAllResourceAccessesAsPublicTest() {
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        performGet(ResourceController.TYPE_MAPPING, publicToken, expectations, "Error retrieving endpoints");
    }

    /**
    *
    * Check that the microservice allow to retrieve all resource endpoints configurations as PROJECT_ADMIN
    *
    * @since 1.0-SNAPSHOT
    */
    @Test
    @Purpose("Check that the microservice allows to retrieve all resource endpoints configurations")
    public void getAllResourceAccessesAsProjectAdminTest() {
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        performGet(ResourceController.TYPE_MAPPING, projectAdminToken, expectations, "Error retrieving endpoints");
    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations as INSTANCE_ADMIN
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    @Purpose("Check that the microservice allows to retrieve all resource endpoints configurations for instance admin")
    public void getAllResourceAccessesAsInstanceAdminTest() {
        final List<ResultMatcher> expectations = new ArrayList<>(3);
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isArray());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        performGet(ResourceController.TYPE_MAPPING, instanceAdminToken, expectations, "Error retrieving endpoints");
    }

    /**
     *
     * Check that the microservice allow to retrieve all resource endpoints configurations
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void getResourceAccessTest() {
        ResourcesAccess resource = new ResourcesAccess("description", DEFAULT_MICROSERVICE, CONFIGURED_ENDPOINT_URL,
                DEFAULT_CONTROLLER, RequestMethod.GET, DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);
        final Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        adminRole.addPermission(resource);
        roleRepository.save(adminRole);

        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_ROOT).isNotEmpty());
        performGet(ResourceController.TYPE_MAPPING + ResourceController.RESOURCE_MAPPING, publicToken, expectations,
                   "Error retrieving endpoints", resource.getId());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
