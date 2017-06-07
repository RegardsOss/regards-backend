/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.rest.test;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
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
 *
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
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, expectations, ERROR_MSG);
    }

    @Test
    public void setMaintenanceTest() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          expectations, ERROR_MSG, TENANT);
    }

    @Test
    public void unSetMaintenanceTest() {
        List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_DESACTIVATE_URL,
                          null, expectations, ERROR_MSG, TENANT);
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_050")
    @Requirement("REGARDS_DSL_CMP_ADM_110")
    @Purpose("In case of HTTP error 503, the microservice is in maintenance state")
    public void controllerUnavailableTest() {
        List<ResultMatcher> expectations = new ArrayList<>();

        // Service unavailable : maintenance mode is set for the tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isServiceUnavailable());
        performDefaultGet(TestController.MAINTENANCE_TEST_503_URL, expectations, ERROR_MSG);

        // control that the service is in maintenance for the tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".active",
                                                        Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".lastUpdate",
                                                        Matchers.notNullValue()));
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, expectations, ERROR_MSG);

        resetMaintenanceMode();
    }

    @Test
    @Requirement("REGARDS_DSL_CMP_ARC_060")
    @Purpose("When a microservice is in maintenance state, only the GET request are performed")
    public void controllerGetRequestWhenUnavailableTest() {
        List<ResultMatcher> expectations = new ArrayList<>();

        // Set the maintenance mode is for the tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          expectations, ERROR_MSG, DEFAULT_TENANT);

        // control that the service is in maintenance for the default tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".active",
                                                        Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".lastUpdate",
                                                        Matchers.notNullValue()));
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, expectations, ERROR_MSG);

        // the GET request should be OK
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(TestController.MAINTENANCE_TEST_URL, expectations, ERROR_MSG);

        // try a POST request : the service is unavailable
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().is(MaintenanceFilter.MAINTENANCE_HTTP_STATUS));
        performDefaultPost(TestController.MAINTENANCE_TEST_URL, null, expectations, ERROR_MSG);

        resetMaintenanceMode();
    }

    @Test
    public void desactivateMaintenanceTest() {
        List<ResultMatcher> expectations = new ArrayList<>();

        // Set the maintenance mode is for the tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          expectations, ERROR_MSG, DEFAULT_TENANT);

        // control that the service is in maintenance for the default tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".active",
                                                        Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".lastUpdate",
                                                        Matchers.notNullValue()));
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, expectations, ERROR_MSG);

        // desactivate the maintenance mode
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_DESACTIVATE_URL,
                          null, expectations, ERROR_MSG, DEFAULT_TENANT);

        // control that the service is not in maintenance for the default tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".active",
                                                        Matchers.hasToString("false")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".lastUpdate",
                                                        Matchers.notNullValue()));
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, expectations, ERROR_MSG);

        // try a POST request : the service is now OK
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performDefaultPost(TestController.MAINTENANCE_TEST_URL, null, expectations, ERROR_MSG, DEFAULT_TENANT);
    }

    @Test
    public void controlMaintenanceWithTwoTenant() {
        List<ResultMatcher> expectations = new ArrayList<>();

        // Set the maintenance mode is for the default tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_ACTIVATE_URL, null,
                          expectations, ERROR_MSG, DEFAULT_TENANT);

        // control that the service is in maintenance for the default tenant
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT).isNotEmpty());
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".active",
                                                        Matchers.hasToString("true")));
        expectations.add(MockMvcResultMatchers.jsonPath(JSON_PATH_CONTENT + "." + DEFAULT_TENANT + ".lastUpdate",
                                                        Matchers.notNullValue()));
        performDefaultGet(MaintenanceController.MAINTENANCE_URL, expectations, ERROR_MSG);

        // control that the service is not in maintenance for the other tenant
        String token = jwtService.generateToken(TENANT, DEFAULT_USER_EMAIL, DEFAULT_ROLE);
        expectations.clear();
        expectations.add(MockMvcResultMatchers.status().isCreated());
        performPost(TestController.MAINTENANCE_TEST_URL, token, null, expectations, ERROR_MSG);

        resetMaintenanceMode();
    }

    private void resetMaintenanceMode() {
        List<ResultMatcher> expectations = new ArrayList<>();
        performDefaultPut(MaintenanceController.MAINTENANCE_URL + MaintenanceController.MAINTENANCE_DESACTIVATE_URL,
                          null, expectations, ERROR_MSG, DEFAULT_TENANT);
    }

}
