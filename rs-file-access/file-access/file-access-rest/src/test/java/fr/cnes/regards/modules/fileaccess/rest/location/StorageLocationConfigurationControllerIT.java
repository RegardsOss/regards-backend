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
package fr.cnes.regards.modules.fileaccess.rest.location;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.fileaccess.dao.IStorageLocationConfigurationRepository;
import fr.cnes.regards.modules.fileaccess.domain.StorageLocationConfiguration;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.fileaccess.rest.StorageLocationConfigurationController;
import fr.cnes.regards.modules.fileaccess.rest.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.fileaccess.service.StorageLocationConfigurationService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

/**
 * @author sbinda
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_location_config_rest_it" })
@ActiveProfiles({ "noscheduler", "nojobs" })
public class StorageLocationConfigurationControllerIT extends AbstractRegardsTransactionalIT {

    private static final String DEFAULT_STORAGE_NAME = "default-storage";

    private static final String STORAGE_PATH = "target/storage/ONLINE-STORAGE";

    @Autowired
    private IStorageLocationConfigurationRepository storageLocationConfigRepository;

    @Autowired
    private StorageLocationConfigurationService storageLocationConfigService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfigRepository;

    private void clear() throws IOException {
        // Delete existing jobs if any
        jobInfoRepository.deleteAll();
        storageLocationConfigRepository.deleteAll();
        pluginConfigRepository.deleteAll();
        if (Files.exists(Paths.get(DEFAULT_STORAGE_NAME))) {
            FileUtils.deleteDirectory(Paths.get(STORAGE_PATH).toFile());
        }
    }

    @Before
    public void init() throws IOException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        clear();
        initDataStoragePluginConfiguration();
    }

    @Test
    public void createStorageConfigLocationOnline() throws IOException {
        // GIVEN
        // new storage config to create
        String name = "online-storage";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                                                                        .expectValue("content.name", name)
                                                                        .expectValue("content.storageType",
                                                                                     StorageType.ONLINE.toString());
        // WHEN / THEN
        performDefaultPost(StorageLocationConfigurationController.BASE_PATH,
                           buildStorageLocationDto(name, 10_000L, false),
                           requestBuilderCustomizer,
                           "Should be created");
    }

    @Test
    public void createStorageConfigLocationOffline() throws IOException {
        // GIVEN
        // new storage config to create
        String name = "offline-storage";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                                                                        .expectValue("content.name", name)
                                                                        .expectValue("content.storageType",
                                                                                     StorageType.OFFLINE.toString());
        // WHEN / THEN
        performDefaultPost(StorageLocationConfigurationController.BASE_PATH,
                           buildStorageLocationDto(name, null, true),
                           requestBuilderCustomizer,
                           "Should be created");
    }

    @Test
    public void configureLocation_alreadyExists() throws IOException {
        // GIVEN
        // new storage config to create
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        performDefaultPost(StorageLocationConfigurationController.BASE_PATH,
                           buildStorageLocationDto("online-storage", null, false),
                           requestBuilderCustomizer,
                           "Should be created");
        // WHEN / THEN
        // the same storage config has to be created
        requestBuilderCustomizer = customizer().expectStatusConflict();
        performDefaultPost(StorageLocationConfigurationController.BASE_PATH,
                           buildStorageLocationDto("online-storage", null, false),
                           requestBuilderCustomizer,
                           "Should not be created");
    }

    @Test
    public void updateLocation() throws IOException {
        // GIVEN
        // new storage config to create
        String name = "online-storage";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                                                                        .expectValue("content.allocatedSizeInKo", 100L);
        performDefaultPost(StorageLocationConfigurationController.BASE_PATH,
                           buildStorageLocationDto(name, 100L, false),
                           requestBuilderCustomizer,
                           "Should be created");
        // retrieve the storage config created
        tenantResolver.forceTenant(getDefaultTenant());
        StorageLocationConfiguration storageConfig = storageLocationConfigService.search(name).orElseThrow();

        // WHEN / THEN
        // update the storage config
        storageConfig.setAllocatedSizeInKo(10_000L);
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("content.allocatedSizeInKo", 10_000L);
        performDefaultPut(StorageLocationConfigurationController.BASE_PATH
                          + StorageLocationConfigurationController.ID_PATH,
                          storageConfig,
                          requestBuilderCustomizer,
                          "Location should be updated",
                          name);
    }

    @Test
    public void retrieveAll() throws IOException {
        // GIVEN
        // new storage config to create
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        performDefaultPost(StorageLocationConfigurationController.BASE_PATH,
                           buildStorageLocationDto("online-storage", null, false),
                           requestBuilderCustomizer,
                           "Should be created");
        // WHEN / THEN
        // Expected 2 results : One created in init method. One created in this test method.
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$").expectToHaveSize("$", 2);
        performDefaultGet(StorageLocationConfigurationController.BASE_PATH,
                          requestBuilderCustomizer,
                          "Expect ok status.");
    }

    @Test
    public void retrieveOne() {
        // retrieve the default storage config created in the init method
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationConfigurationController.BASE_PATH
                          + StorageLocationConfigurationController.ID_PATH,
                          requestBuilderCustomizer,
                          "Expect ok status.",
                          DEFAULT_STORAGE_NAME);
    }

    @Test
    public void retrieveOneNotExisting() {
        // retrieve the default storage config created in the init method
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusNotFound();
        performDefaultGet(StorageLocationConfigurationController.BASE_PATH
                          + StorageLocationConfigurationController.ID_PATH,
                          requestBuilderCustomizer,
                          "Expect not found status.",
                          "unknown");
    }

    @Test
    public void delete() {
        // delete the default storage config created in the init method
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationConfigurationController.BASE_PATH
                             + StorageLocationConfigurationController.ID_PATH,
                             requestBuilderCustomizer,
                             "Expect ok status.",
                             DEFAULT_STORAGE_NAME);
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private PluginConfiguration getPluginConfig(String name) throws IOException {
        PluginMetaData pluginMetadata = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
        Set<IPluginParam> parameters = IPluginParam.set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME,
                                                                           STORAGE_PATH),
                                                        IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN,
                                                                           "error.*"),
                                                        IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN,
                                                                           "delErr.*"));
        return new PluginConfiguration(pluginMetadata.getPluginId(),
                                       name,
                                       UUID.randomUUID().toString(),
                                       "1.0",
                                       0,
                                       true,
                                       null,
                                       parameters,
                                       pluginMetadata);
    }

    private void initDataStoragePluginConfiguration() throws ModuleException {
        try {
            storageLocationConfigService.create(DEFAULT_STORAGE_NAME,
                                                getPluginConfig(DEFAULT_STORAGE_NAME),
                                                1_000_000L);
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

    private StorageLocationConfigurationDto buildStorageLocationDto(String name,
                                                                    Long allocatedSizeInKo,
                                                                    boolean offline) throws IOException {
        return new StorageLocationConfiguration(name,
                                                offline ? null : getPluginConfig(name),
                                                allocatedSizeInKo).toDto();
    }

}
