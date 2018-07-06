package fr.cnes.regards.modules.storage.rest;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.microservice.rest.MicroserviceConfigurationController;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsIT;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.notification.client.INotificationClient;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IAllocationStrategy;
import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;
import fr.cnes.regards.modules.storage.service.StorageConfigurationManager;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class StorageConfigurationManagerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IPrioritizedDataStorageService prioritizedDataStorageService;

    @Autowired
    private IPluginService pluginService;

    @Test
    public void testExport() throws ModuleException, IOException, URISyntaxException {
        // lets create some plugins to export:
        // prioritized data storages
        String pdsLabel1 = StorageConfigurationManagerIT.class.getName() + "_PDS1";
        PrioritizedDataStorage pds1 = createPrioritizedDataStorage(pdsLabel1);

        String pdsLabel2 = StorageConfigurationManagerIT.class.getName() + "_PDS2";
        PrioritizedDataStorage pds2 = createPrioritizedDataStorage(pdsLabel2);

        String pdsLabel3 = StorageConfigurationManagerIT.class.getName() + "_PDS3";
        PrioritizedDataStorage pds3 = createPrioritizedDataStorage(pdsLabel3);
        // Allocation Strategy
        String allocationStrategyLabel = StorageConfigurationManagerIT.class.getName() + "_AS";
        PluginConfiguration allocationStrategy = createAllocationStrategy(allocationStrategyLabel);
        // Security Delegation
        String securityDelegationLabel = StorageConfigurationManagerIT.class.getName() + "_SD";
        PluginConfiguration securityDelegation = createSecurityDelegation(securityDelegationLabel);

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());

        performDefaultGet(MicroserviceConfigurationController.TYPE_MAPPING, requestBuilderCustomizer,
                          "Should export configuration");

    }

    @Test
    public void testImport() {
        Path filePath = Paths.get("src", "test", "resources", "storage-configuration.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());

        performDefaultFileUpload(MicroserviceConfigurationController.TYPE_MAPPING, filePath, requestBuilderCustomizer,
                                 "Should be able to import configuration");
    }

    private PrioritizedDataStorage createPrioritizedDataStorage(String label)
            throws IOException, URISyntaxException, ModuleException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class,
                                                                      IDataStorage.class.getPackage().getName(),
                                                                      IOnlineDataStorage.class.getPackage().getName());
        URL baseStorageLocation = new URL("file",
                                          "",
                                          Paths.get("target/PrioritizedDataStorageServiceIT").toFile()
                                                  .getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L)
                .addParameter(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, baseStorageLocation.toString())
                .getParameters();
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        return prioritizedDataStorageService.create(dataStorageConf);
    }

    private PluginConfiguration createAllocationStrategy(String label) throws ModuleException {
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class,
                                                                         DefaultAllocationStrategyPlugin.class
                                                                                 .getPackage().getName(),
                                                                         IAllocationStrategy.class.getPackage()
                                                                                 .getName());
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta,
                                                                              label,
                                                                              new ArrayList<>(),
                                                                              0);
        allocationConfiguration.setIsActive(true);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        return pluginService.savePluginConfiguration(allocationConfiguration);
    }

    private PluginConfiguration createSecurityDelegation(String label) throws ModuleException {
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(FakeSecurityDelegation.class,
                                                                               FakeSecurityDelegation.class.getPackage()
                                                                                       .getName(),
                                                                               ISecurityDelegation.class.getPackage()
                                                                                       .getName());
        PluginConfiguration catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, label);
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        return pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

    @Configuration
    static class Config {

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }

        @Bean
        public INotificationClient notificationClient() {
            return Mockito.mock(INotificationClient.class);
        }
    }

}
