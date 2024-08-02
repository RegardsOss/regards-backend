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
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.accessrights.dao.projects.IResourcesAccessRepository;
import fr.cnes.regards.modules.accessrights.dao.projects.IRoleRepository;
import fr.cnes.regards.modules.accessrights.domain.projects.ResourcesAccess;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;
import fr.cnes.regards.modules.accessrights.service.projectuser.QuotaHelperService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Class ResourceControllerIT
 * <p>
 * Test class to check access to {@link ResourcesAccess} entities. Those entities are used to configure the authroized
 * access to microservices endpoints.
 *
 * @author Sébastien Binda
 */
@MultitenantTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=account" })
public class ResourceControllerIT extends AbstractRegardsTransactionalIT {

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

    @MockBean
    private QuotaHelperService quotaHelperService;

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
     * Initialize all datas for this unit tests
     */
    @Before
    public void initResources() {

        JWTService service = new JWTService();
        service.setSecret("!!!!!==========abcdefghijklmnopqrstuvwxyz0123456789==========!!!!!");
        publicToken = service.generateToken(getDefaultTenant(), getDefaultUserEmail(), DefaultRole.PUBLIC.toString());
        projectAdminToken = service.generateToken(getDefaultTenant(),
                                                  getDefaultUserEmail(),
                                                  DefaultRole.PROJECT_ADMIN.toString());
        instanceAdminToken = service.generateToken(getDefaultTenant(),
                                                   getDefaultUserEmail(),
                                                   DefaultRole.INSTANCE_ADMIN.toString());
    }

    /**
     * Check that the microservice allow to retrieve all resource endpoints configurations as PUBLIC
     */
    @Test
    @Purpose("Check that the microservice allows to retrieve all resource endpoints configurations")
    public void getAllResourceAccessesAsPublicTest() {
        performGet(ResourceController.TYPE_MAPPING,
                   publicToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_CONTENT).expectIsNotEmpty(JSON_PATH_CONTENT),
                   "Error retrieving endpoints");
    }

    /**
     * Check that the microservice allow to retrieve all resource endpoints configurations as PROJECT_ADMIN
     */
    @Test
    @Purpose("Check that the microservice allows to retrieve all resource endpoints configurations")
    public void getAllResourceAccessesAsProjectAdminTest() {
        performGet(ResourceController.TYPE_MAPPING,
                   projectAdminToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_CONTENT).expectIsNotEmpty(JSON_PATH_CONTENT),
                   "Error retrieving endpoints");
    }

    /**
     * Check that the microservice allow to retrieve all resource endpoints configurations as INSTANCE_ADMIN
     */
    @Test
    @Purpose("Check that the microservice allows to retrieve all resource endpoints configurations for instance admin")
    public void getAllResourceAccessesAsInstanceAdminTest() {
        performGet(ResourceController.TYPE_MAPPING,
                   instanceAdminToken,
                   customizer().expectStatusOk().expectIsArray(JSON_PATH_CONTENT).expectIsNotEmpty(JSON_PATH_CONTENT),
                   "Error retrieving endpoints");
    }

    /**
     * Check that the microservice allow to retrieve all resource endpoints configurations
     */
    @Test
    public void getResourceAccessTest() {
        ResourcesAccess resource = new ResourcesAccess("description",
                                                       DEFAULT_MICROSERVICE,
                                                       CONFIGURED_ENDPOINT_URL,
                                                       DEFAULT_CONTROLLER,
                                                       RequestMethod.GET,
                                                       DefaultRole.ADMIN);
        resourcesAccessRepository.save(resource);
        Role adminRole = roleRepository.findOneByName(DefaultRole.ADMIN.toString()).get();
        adminRole.addPermission(resource);
        roleRepository.save(adminRole);

        performGet(ResourceController.TYPE_MAPPING + ResourceController.RESOURCE_MAPPING,
                   publicToken,
                   customizer().expectStatusOk().expectIsNotEmpty(JSON_PATH_ROOT),
                   "Error retrieving endpoints",
                   resource.getId());
    }

}
