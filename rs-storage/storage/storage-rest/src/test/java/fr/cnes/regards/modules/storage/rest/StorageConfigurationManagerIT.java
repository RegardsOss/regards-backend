/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.microservice.rest.ModuleManagerController;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;
import fr.cnes.regards.modules.storage.plugin.allocation.strategy.DefaultAllocationStrategyPlugin;
import fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage;
import fr.cnes.regards.modules.storage.service.IPrioritizedDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class StorageConfigurationManagerIT extends AbstractRegardsTransactionalIT {

    @Configuration
    static class Config {

        @Bean
        public IProjectsClient projectsClient() {
            return Mockito.mock(IProjectsClient.class);
        }

    }

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
        createPrioritizedDataStorage(pdsLabel1);

        String pdsLabel2 = StorageConfigurationManagerIT.class.getName() + "_PDS2";
        createPrioritizedDataStorage(pdsLabel2);

        String pdsLabel3 = StorageConfigurationManagerIT.class.getName() + "_PDS3";
        createPrioritizedDataStorage(pdsLabel3);

        // Allocation Strategy
        String allocationStrategyLabel = StorageConfigurationManagerIT.class.getName() + "_AS";
        createAllocationStrategy(allocationStrategyLabel);

        // Security Delegation
        String securityDelegationLabel = StorageConfigurationManagerIT.class.getName() + "_SD";
        createSecurityDelegation(securityDelegationLabel);

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();

        performDefaultGet(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                          requestBuilderCustomizer,
                          "Should export configuration");

    }

    @Test
    public void testImport() {
        Path filePath = Paths.get("src", "test", "resources", "storage-configuration.json");

        // Define expectations
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();

        performDefaultFileUpload(ModuleManagerController.TYPE_MAPPING + ModuleManagerController.CONFIGURATION_MAPPING,
                                 filePath,
                                 requestBuilderCustomizer,
                                 "Should be able to import configuration");
    }

    private PrioritizedDataStorage createPrioritizedDataStorage(String label)
            throws IOException, URISyntaxException, ModuleException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(LocalDataStorage.class);
        URL baseStorageLocation = new URL("file",
                                          "",
                                          Paths.get("target/PrioritizedDataStorageServiceIT").toFile()
                                                  .getAbsolutePath());
        Files.createDirectories(Paths.get(baseStorageLocation.toURI()));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(LocalDataStorage.LOCAL_STORAGE_TOTAL_SPACE, 9000000000000000L),
                     IPluginParam.build(LocalDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                        baseStorageLocation.toString()));
        PluginConfiguration dataStorageConf = new PluginConfiguration(dataStoMeta, label, parameters, 0);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        return prioritizedDataStorageService.create(dataStorageConf);
    }

    private PluginConfiguration createAllocationStrategy(String label) throws ModuleException {
        PluginMetaData allocationMeta = PluginUtils.createPluginMetaData(DefaultAllocationStrategyPlugin.class);
        PluginConfiguration allocationConfiguration = new PluginConfiguration(allocationMeta,
                                                                              label,
                                                                              new ArrayList<>(),
                                                                              0);
        allocationConfiguration.setIsActive(true);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        return pluginService.savePluginConfiguration(allocationConfiguration);
    }

    private PluginConfiguration createSecurityDelegation(String label) throws ModuleException {
        PluginMetaData catalogSecuDelegMeta = PluginUtils.createPluginMetaData(FakeSecurityDelegation.class);
        PluginConfiguration catalogSecuDelegConf = new PluginConfiguration(catalogSecuDelegMeta, label);
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        return pluginService.savePluginConfiguration(catalogSecuDelegConf);
    }

}
