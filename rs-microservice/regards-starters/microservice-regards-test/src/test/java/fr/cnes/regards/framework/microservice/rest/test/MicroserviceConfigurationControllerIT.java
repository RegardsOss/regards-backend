package fr.cnes.regards.framework.microservice.rest.test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.microservice.rest.MicroserviceConfigurationController;
import fr.cnes.regards.framework.microservice.rest.test.domain.ConfigurationPojo;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.ConstrainedFields;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class MicroserviceConfigurationControllerIT extends AbstractRegardsIT {

    @Autowired
    private TestConfigurationManager testConfigurationManager;

    @Configuration
    static class Config {

        @Bean
        public TestConfigurationManager testConfigurationManager() {
            return new TestConfigurationManager();
        }

    }

    @Test
    public void testExport() {
        // lets request export from REST endpoint
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(MicroserviceConfigurationController.TYPE_MAPPING, requestBuilderCustomizer,
                          "Should export configuration");
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

        performDefaultFileUpload(MicroserviceConfigurationController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
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

        performDefaultFileUpload(MicroserviceConfigurationController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
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

        performDefaultFileUpload(MicroserviceConfigurationController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
    }

}
