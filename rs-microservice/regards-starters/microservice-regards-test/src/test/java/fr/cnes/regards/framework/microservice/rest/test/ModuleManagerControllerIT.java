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
        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                          customizer().expectStatusOk(), "Should export configuration");
    }

    @Test
    public void testImportOk() {
        testConfigurationManager.setPartialFail(false);
        testConfigurationManager.setTotalFail(false);
        //lets request some import

        Path filePath = Paths.get("src", "test", "resources", "test-configuration.json");

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, customizer().expectStatusCreated(), "Should be able to import configuration");
    }

    @Test
    public void testImportPartial() {
        testConfigurationManager.setPartialFail(true);
        testConfigurationManager.setTotalFail(false);
        //lets request some import

        Path filePath = Paths.get("src", "test", "resources", "test-configuration.json");

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, customizer().expect(MockMvcResultMatchers.status().isPartialContent()),
                                 "Should be able to import configuration");
    }

    @Test
    public void testImportFail() {
        testConfigurationManager.setPartialFail(false);
        testConfigurationManager.setTotalFail(true);
        //lets request some import

        Path filePath = Paths.get("src", "test", "resources", "test-configuration.json");

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath, customizer().expect(MockMvcResultMatchers.status().isConflict()),
                                 "Should be able to import configuration");
    }

    @Test
    public void testReady() {
        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.READY_MAPPING,
                          customizer().expectStatusOk(), "Ready endpoint should be reached!");
    }

    @Test
    public void testRestart() {
        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.RESTART_MAPPING,
                          customizer().expectStatusOk(), "Restart endpoint should be reached!");
    }

    @Configuration
    static class Config {

        @Bean
        public TestConfigurationManager testConfigurationManager() {
            return new TestConfigurationManager();
        }

    }

}
