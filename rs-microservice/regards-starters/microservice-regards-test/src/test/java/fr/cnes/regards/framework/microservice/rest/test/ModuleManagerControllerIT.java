package fr.cnes.regards.framework.microservice.rest.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.microservice.rest.ModuleManagerController;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@ContextConfiguration(classes = ModuleManagerControllerIT.Config.class)
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class ModuleManagerControllerIT extends AbstractRegardsIT {

    @Autowired
    private TestConfigurationManager testConfigurationManager;

    @Test
    public void testExport() {
        // lets request export from REST endpoint
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                          requestBuilderCustomizer, "Should export configuration");
    }

    @Test
    public void testImportOk() {
        testConfigurationManager.setPartialFail(false);
        testConfigurationManager.setTotalFail(false);
        //lets request some import

        Path filePath = Paths.get("src", "test", "resources", "test-configuration.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");
    }

    @Test
    public void testImportPartial() {
        testConfigurationManager.setPartialFail(true);
        testConfigurationManager.setTotalFail(false);
        //lets request some import

        Path filePath = Paths.get("src", "test", "resources", "test-configuration.json");
        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent());

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");
    }

    @Test
    public void testImportFail() {
        testConfigurationManager.setPartialFail(false);
        testConfigurationManager.setTotalFail(true);
        //lets request some import

        Path filePath = Paths.get("src", "test", "resources", "test-configuration.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isConflict());

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, requestBuilderCustomizer, "Should be able to import configuration");
    }

    @Test
    public void testReady() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.READY_MAPPING,
                          requestBuilderCustomizer, "Ready endpoint should be reached!");
    }

    @Test
    public void testRestart() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.RESTART_MAPPING,
                          requestBuilderCustomizer, "Restart endpoint should be reached!");
    }

    @Configuration
    static class Config {

        @Bean
        public TestConfigurationManager testConfigurationManager() {
            return new TestConfigurationManager();
        }

    }

}
