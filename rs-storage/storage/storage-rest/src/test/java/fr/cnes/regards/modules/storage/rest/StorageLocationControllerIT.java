/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.storage.domain.database.StorageLocationConfiguration;
import fr.cnes.regards.modules.storage.domain.dto.CopyFilesParametersDTO;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.rest.plugin.SimpleOnlineDataStorage;
import fr.cnes.regards.modules.storage.service.location.StorageLocationConfigurationService;
import fr.cnes.regards.modules.storage.service.location.StorageLocationService;

/**
 * @author sbinda
 *
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_loc_rest_it",
        "regards.storage.cache.path=target/cache" })
public class StorageLocationControllerIT extends AbstractRegardsTransactionalIT {

    private static final String TARGET_STORAGE = "target";

    private static final String STORAGE_PATH = "target/ONLINE-STORAGE";

    @Autowired
    private StorageLocationConfigurationService storageLocationConfService;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private StorageLocationService storageLocService;

    private void clear() throws IOException {
        // Delete existing jobs if any
        jobInfoRepository.deleteAll();

        for (StorageLocationConfiguration loc : storageLocationConfService.searchAll()) {
            try {
                storageLocationConfService.delete(loc.getId());
            } catch (ModuleException e) {
                Assert.fail(e.getMessage());
            }
        }
        if (Files.exists(Paths.get("target/storage"))) {
            FileUtils.deleteDirectory(Paths.get(STORAGE_PATH).toFile());
        }
    }

    @Before
    public void init()
            throws NoSuchAlgorithmException, FileNotFoundException, IOException, InterruptedException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        clear();
        initDataStoragePluginConfiguration();
    }

    @Test
    public void configureLocation() throws IOException {
        String name = "plop";

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                .expectValue("content.name", name).expectValue("content.nbFilesStored", 0)
                .expectValue("content.totalStoredFilesSizeKo", 0).expectValue("content.nbStorageError", 0)
                .expectValue("content.configuration.name", name)
                .expectValue("content.configuration.storageType", StorageType.OFFLINE.toString());
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO.build(name, null, null, null, null, false, false, false,
                                                    new StorageLocationConfiguration(name, null, null)),
                           requestBuilderCustomizer, "Should be created");

        name = "plop2";
        requestBuilderCustomizer = customizer().expectStatusCreated().expectValue("content.name", name)
                .expectValue("content.nbFilesStored", 0).expectValue("content.totalStoredFilesSizeKo", 0)
                .expectValue("content.nbStorageError", 0).expectValue("content.configuration.name", name)
                .expectValue("content.configuration.storageType", StorageType.ONLINE.toString());
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO
                                   .build("plop2", null, null, null, null, false, false, false,
                                          new StorageLocationConfiguration("plop2", getPluginConf("plop2"), 10_000L)),
                           requestBuilderCustomizer, "Should be created");
    }

    @Test
    public void configureLocation_alreadyExists() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO.build("plop", null, null, null, null, false, false, false,
                                                    new StorageLocationConfiguration("plop", null, null)),
                           requestBuilderCustomizer, "Should be created");

        requestBuilderCustomizer = customizer().expectStatusConflict();
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO.build("plop", null, null, null, null, false, false, false,
                                                    new StorageLocationConfiguration("plop", null, null)),
                           requestBuilderCustomizer, "Should not be created");
    }

    @Test
    public void updateLocation() throws IOException, EntityNotFoundException {
        String name = "name";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                .expectValue("content.configuration.allocatedSizeInKo", 100L);
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO.build(name, null, null, null, null, false, false, false,
                                                    new StorageLocationConfiguration(name, getPluginConf(name), 100L)),
                           requestBuilderCustomizer, "Should be created");

        tenantResolver.forceTenant(getDefaultTenant());
        StorageLocationDTO loc = storageLocService.getById(name);
        loc.getConfiguration().setAllocatedSizeInKo(10_000L);
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("content.configuration.allocatedSizeInKo",
                                                                             10_000L);
        performDefaultPut(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH, loc,
                          requestBuilderCustomizer, "Location should be updated", name);
    }

    @Test
    public void retreiveAll() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO.build("plop", null, null, null, null, false, false, false,
                                                    new StorageLocationConfiguration("plop", null, null)),
                           requestBuilderCustomizer, "Should be created");

        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$").expectToHaveSize("$", 2);
        performDefaultGet(StorageLocationController.BASE_PATH, requestBuilderCustomizer, "Expect ok status.");
    }

    @Test
    public void retreiveOne() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void retryErrors() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH
                + StorageLocationController.FILES + StorageLocationController.RETRY, requestBuilderCustomizer,
                          "Expect ok status.", TARGET_STORAGE, FileRequestType.STORAGE.toString());
    }

    @Test
    public void copyFiles() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultPost(StorageLocationController.BASE_PATH + StorageLocationController.FILES
                + StorageLocationController.COPY,
                           CopyFilesParametersDTO.build("somewhere", "/dir/one", "somewhere-else", null),
                           requestBuilderCustomizer, "Expect ok status.");
    }

    @Test
    public void delete() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                             requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void deleteFiles() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH
                + StorageLocationController.FILES, requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void increasePriority() throws IOException {
        // Create a second ONLINE configuration
        String name = "name";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                .expectValue("content.configuration.allocatedSizeInKo", 10_000L)
                .expectValue("content.configuration.priority", 1);
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO
                                   .build(name, null, null, null, null, false, false, false,
                                          new StorageLocationConfiguration(name, getPluginConf(name), 10_000L)),
                           requestBuilderCustomizer, "Should be created");

        // Ask for priority up
        requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultPut(StorageLocationController.BASE_PATH + StorageLocationController.UP_PATH, null,
                          requestBuilderCustomizer, "Expect ok status.", name);

        // Check new conf priority increase from 1 to 0
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("content.configuration.priority", 0);
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer, "Expect ok status.", name);

        // Check old conf priority decrease from 0 to 1
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("content.configuration.priority", 1);
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    @Test
    public void decreasePriority() throws IOException {
        // Create a second ONLINE configuration
        String name = "name";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                .expectValue("content.configuration.allocatedSizeInKo", 10_000L)
                .expectValue("content.configuration.priority", 1);
        performDefaultPost(StorageLocationController.BASE_PATH,
                           StorageLocationDTO
                                   .build(name, null, null, null, null, false, false, false,
                                          new StorageLocationConfiguration(name, getPluginConf(name), 10_000L)),
                           requestBuilderCustomizer, "Should be created");

        // Ask for priority down on previous conf
        requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultPut(StorageLocationController.BASE_PATH + StorageLocationController.DOWN_PATH, null,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);

        // Check new conf priority increase from 1 to 0
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("content.configuration.priority", 0);
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer, "Expect ok status.", name);

        // Check old conf priority decrease from 0 to 1
        requestBuilderCustomizer = customizer().expectStatusOk().expectValue("content.configuration.priority", 1);
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer, "Expect ok status.", TARGET_STORAGE);
    }

    private PluginConfiguration getPluginConf(String name) throws IOException {
        PluginMetaData dataStoMeta = PluginUtils.createPluginMetaData(SimpleOnlineDataStorage.class);
        // Files.createDirectories(Paths.get(STORAGE_PATH));
        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(SimpleOnlineDataStorage.BASE_STORAGE_LOCATION_PLUGIN_PARAM_NAME, STORAGE_PATH),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_STORAGE_ERROR_FILE_PATTERN, "error.*"),
                     IPluginParam.build(SimpleOnlineDataStorage.HANDLE_DELETE_ERROR_FILE_PATTERN, "delErr.*"));
        return new PluginConfiguration(name, parameters, 0, dataStoMeta.getPluginId());
    }

    private void initDataStoragePluginConfiguration() throws ModuleException {
        try {
            storageLocationConfService.create(TARGET_STORAGE, getPluginConf(TARGET_STORAGE), 1_000_000L);
        } catch (IOException e) {
            throw new ModuleException(e.getMessage(), e);
        }
    }

}
