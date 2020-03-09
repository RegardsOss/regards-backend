/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.microservice.rest.test;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.microservice.maintenance.MaintenanceFilter;
import fr.cnes.regards.framework.microservice.rest.MaintenanceController;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.maintenancetest.rest.TestController;

/**
 * @author Sylvain Vissiere-Guerinet
 * @author Christophe Mertz
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class MaintenanceControllerIT extends AbstractRegardsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceControllerIT.class);

    private static final String ERROR_MSG = "Error during the request";

    private static final String TENANT = "tenant1";

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Test
    public void retrieveTenantsInMaintenanceTest() {
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, customizer().expectStatusOk(), ERROR_MSG);
    }

    @Test
    public void setMaintenanceTest() {
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          customizer().expectStatusOk(), ERROR_MSG, TENANT);
    }

    @Test
    public void unSetMaintenanceTest() {
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_DESACTIVATE_URL,
                          null, customizer().expectStatusOk(), ERROR_MSG, TENANT);
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_050")
    @Requirement("REGARDS_DSL_CMP_ADM_110")
    @Purpose("In case of HTTP error 503, the microservice is in maintenance state")
    public void controllerUnavailableTest() {
        // Service unavailable : maintenance mode is set for the tenant
        performDefaultGet(TestController.MAINTENANCE_TEST_503_URL,
                          customizer().expect(MockMvcResultMatchers.status().isServiceUnavailable()), ERROR_MSG);

        // control that the service is in maintenance for the tenant
        String jsonTenantContent = JSON_PATH_CONTENT + "." + getDefaultTenant();
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty())
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".active", Matchers.hasToString("true")))
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".lastUpdate", Matchers.notNullValue())),
                          ERROR_MSG);

        resetMaintenanceMode();
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_060")
    @Purpose("When a microservice is in maintenance state, only the GET request are performed")
    public void controllerGetRequestWhenUnavailableTest() {
        // Set the maintenance mode is for the tenant
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          customizer().expectStatusOk(), ERROR_MSG, getDefaultTenant());

        // control that the service is in maintenance for the default tenant
        String jsonTenantContent = JSON_PATH_CONTENT + "." + getDefaultTenant();
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty())
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".active", Matchers.hasToString("true")))
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".lastUpdate", Matchers.notNullValue())),
                          ERROR_MSG);

        // the GET request should be OK
        performDefaultGet(TestController.MAINTENANCE_TEST_URL, customizer().expectStatusOk(), ERROR_MSG);

        // try a POST request : the service is unavailable
        // we skip documentation because status code 515 is not standard and Spring does not handle it.
        performDefaultPost(TestController.MAINTENANCE_TEST_URL, null,
                           customizer()
                                   .expect(MockMvcResultMatchers.status().is(MaintenanceFilter.MAINTENANCE_HTTP_STATUS))
                                   .skipDocumentation(),
                           ERROR_MSG);

        resetMaintenanceMode();
    }

    @Test
    public void desactivateMaintenanceTest() {
        // Set the maintenance mode is for the tenant
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          customizer().expectStatusOk(), ERROR_MSG, getDefaultTenant());

        // control that the service is in maintenance for the default tenant
        String jsonTenantContent = JSON_PATH_CONTENT + "." + getDefaultTenant();
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty())
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".active", Matchers.hasToString("true")))
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".lastUpdate", Matchers.notNullValue())),
                          ERROR_MSG);

        // desactivate the maintenance mode
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_DESACTIVATE_URL,
                          null, customizer().expectStatusOk(), ERROR_MSG, getDefaultTenant());

        // control that the service is not in maintenance for the default tenant
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty())
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".active", Matchers.hasToString("false")))
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".lastUpdate", Matchers.notNullValue())),
                          ERROR_MSG);

        // try a POST request : the service is now OK
        performDefaultPost(TestController.MAINTENANCE_TEST_URL, null, customizer().expectStatusCreated(), ERROR_MSG,
                           getDefaultTenant());
    }

    @Test
    public void controlMaintenanceWithTwoTenant() {
        // Set the maintenance mode is for the default tenant
        String jsonTenantContent = JSON_PATH_CONTENT + "." + getDefaultTenant();
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          customizer().expectStatusOk(), ERROR_MSG, getDefaultTenant());

        // control that the service is in maintenance for the default tenant
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, customizer().expectStatusOk()
                .expect(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty())
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".active", Matchers.hasToString("true")))
                .expect(MockMvcResultMatchers.jsonPath(jsonTenantContent + ".lastUpdate", Matchers.notNullValue())),
                          ERROR_MSG);

        // control that the service is not in maintenance for the other tenant
        String token = jwtService.generateToken(TENANT, getDefaultUserEmail(), getDefaultUserEmail(), DEFAULT_ROLE);
        performPost(TestController.MAINTENANCE_TEST_URL, token, null,
                    customizer().expectStatusCreated().addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE),
                    ERROR_MSG);

        resetMaintenanceMode();
    }

    private void resetMaintenanceMode() {
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_DESACTIVATE_URL,
                          null, customizer().expectStatusOk(), ERROR_MSG, getDefaultTenant());
    }

}
