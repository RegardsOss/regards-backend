/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.microservice.rest.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class MaintenanceControllerIT extends AbstractRegardsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceControllerIT.class);

    private static final String MAINTENANCES_URL = "/maintenances";

    private static final String MAINTENANCES_ACTIVATE_URL = MAINTENANCES_URL + "/{tenant}/activate";

    private static final String MAINTENANCES_DESACTIVATE_URL = MAINTENANCES_URL + "/{tenant}/desactivate";

    private static final String ERROR_MSG = "Error during the request";

    private static final String TENANT = "tenant1";

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Test
    public void TestRetrieveTenantsInMaintenance() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultGet(MAINTENANCES_URL, expectations, ERROR_MSG);
    }

    @Test
    public void TestSetMaintenance() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MAINTENANCES_ACTIVATE_URL, null, expectations, ERROR_MSG, TENANT);
    }

    @Test
    public void TestUnSetMaintenance() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(MockMvcResultMatchers.status().isOk());
        performDefaultPut(MAINTENANCES_DESACTIVATE_URL, null, expectations, ERROR_MSG, TENANT);
    }

}
