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
package fr.cnes.regards.modules.filecatalog.rest;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.plugins.dto.PluginConfigurationDto;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.fileaccess.client.IStorageLocationConfigurationClient;
import fr.cnes.regards.modules.fileaccess.dto.FileRequestType;
import fr.cnes.regards.modules.fileaccess.dto.StorageLocationConfigurationDto;
import fr.cnes.regards.modules.fileaccess.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.filecatalog.domain.StorageLocation;
import fr.cnes.regards.modules.filecatalog.dto.StorageLocationDto;
import fr.cnes.regards.modules.filecatalog.service.location.StorageLocationService;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

/**
 * Test for {@link StorageLocationController}
 *
 * @author Thibaud Michaudel
 * @author sbinda
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=filecatalog_storage_location_rest_it" })
@ActiveProfiles({ "noscheduler", "nojobs" })
public class StorageLocationControllerIT extends AbstractRegardsTransactionalIT {

    private static final String TARGET_STORAGE = "target";

    private static final String STORAGE_PATH = "target/ONLINE-STORAGE";

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private StorageLocationService storageLocService;

    @MockBean
    private IStorageLocationConfigurationClient storageLocationConfigurationClient;

    @MockBean
    private IStorageLocationRepository storageLocationRepository;

    private void clear() throws IOException {
        // Delete existing jobs if any
        jobInfoRepository.deleteAll();

        if (Files.exists(Paths.get("target/storage"))) {
            FileUtils.deleteDirectory(Paths.get(STORAGE_PATH).toFile());
        }
    }

    @Before
    public void init() throws NoSuchAlgorithmException, IOException, InterruptedException, ModuleException {
        tenantResolver.forceTenant(getDefaultTenant());
        clear();
        mockStorageCreation(TARGET_STORAGE, StorageType.ONLINE);
    }

    @Test
    public void configureLocation() throws IOException, ModuleException {
        String name = "plop";

        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                                                                        .expectValue("content.name", name)
                                                                        .expectValue("content.nbFilesStored", 0)
                                                                        .expectValue("content.totalStoredFilesSizeKo",
                                                                                     0)
                                                                        .expectValue("content.nbStorageError", 0)
                                                                        .expectValue("content.configuration.name", name)
                                                                        .expectValue("content.configuration.storageType",
                                                                                     StorageType.ONLINE.toString());
        mockStorageCreation(name, StorageType.ONLINE);
        performDefaultPost(StorageLocationController.BASE_PATH,
                           buildStorageLocationDto(name),
                           requestBuilderCustomizer,
                           "Should be created");

        name = "plop2";
        requestBuilderCustomizer = customizer().expectStatusCreated()
                                               .expectValue("content.name", name)
                                               .expectValue("content.nbFilesStored", 0)
                                               .expectValue("content.totalStoredFilesSizeKo", 0)
                                               .expectValue("content.nbStorageError", 0)
                                               .expectValue("content.configuration.name", name)
                                               .expectValue("content.configuration.storageType",
                                                            StorageType.ONLINE.toString());
        mockStorageCreation(name, StorageType.ONLINE);
        performDefaultPost(StorageLocationController.BASE_PATH,
                           buildStorageLocationDto("plop2"),
                           requestBuilderCustomizer,
                           "Should be created");
    }

    @Test
    public void updateLocation() throws IOException, ModuleException {
        String name = "name";
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated()
                                                                        .expectValue(
                                                                            "content.configuration.allocatedSizeInKo",
                                                                            0L);
        mockStorageCreation(name, StorageType.ONLINE);

        tenantResolver.forceTenant(getDefaultTenant());

        mockUpdate(name);
        requestBuilderCustomizer = customizer().expectStatusOk()
                                               .expectValue("content.configuration.allocatedSizeInKo", 10_000L);
        performDefaultPut(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          new StorageLocation(name),
                          requestBuilderCustomizer,
                          "Location should be updated",
                          name);
    }

    @Test
    public void retrieveAll() throws IOException, ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusCreated();

        String storageName = "plop";

        mockStorageCreation(storageName, StorageType.ONLINE);

        performDefaultPost(StorageLocationController.BASE_PATH,
                           buildStorageLocationDto(storageName),
                           requestBuilderCustomizer,
                           "Should be created");

        // Expected 3 results : One created in init mehod. One created in this test method. One default cache system.
        mockGetAll(storageName, TARGET_STORAGE);
        requestBuilderCustomizer = customizer().expectStatusOk().expectIsArray("$").expectToHaveSize("$", 2);
        performDefaultGet(StorageLocationController.BASE_PATH, requestBuilderCustomizer, "Expect ok status.");
    }

    @Test
    public void retrieveOne() throws ModuleException {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        mockGet(TARGET_STORAGE);
        performDefaultGet(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                          requestBuilderCustomizer,
                          "Expect ok status.",
                          TARGET_STORAGE);
    }

    @Test
    public void retryErrors() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultGet(StorageLocationController.BASE_PATH
                          + StorageLocationController.ID_PATH
                          + StorageLocationController.FILES
                          + StorageLocationController.RETRY,
                          requestBuilderCustomizer,
                          "Expect ok status.",
                          TARGET_STORAGE,
                          FileRequestType.STORAGE.toString());
    }

    @Test
    public void delete() throws ModuleException {
        mockDelete();
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationController.BASE_PATH + StorageLocationController.ID_PATH,
                             requestBuilderCustomizer,
                             "Expect ok status.",
                             TARGET_STORAGE);
    }

    @Test
    public void deleteFiles() {
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        performDefaultDelete(StorageLocationController.BASE_PATH
                             + StorageLocationController.ID_PATH
                             + StorageLocationController.FILES,
                             requestBuilderCustomizer,
                             "Expect ok status.",
                             TARGET_STORAGE);
    }

    private StorageLocationDto buildStorageLocationDto(String name) throws IOException {
        PluginConfigurationDto pluginConfigurationDto = new PluginConfigurationDto("storagePluginId",
                                                                                   "storagePluginLabel",
                                                                                   "storagePluginBusinessId",
                                                                                   "1.0",
                                                                                   0,
                                                                                   true,
                                                                                   new URL("http://icon.com"),
                                                                                   new HashSet<>(),
                                                                                   new PluginMetaData());

        StorageLocationConfigurationDto storageLocationConfigurationDto = new StorageLocationConfigurationDto(name,
                                                                                                              pluginConfigurationDto,
                                                                                                              0L,
                                                                                                              0L);
        return StorageLocationDto.build(name, storageLocationConfigurationDto);
    }

    private void mockStorageCreation(String storageName, StorageType online) throws ModuleException {
        Mockito.when(storageLocationConfigurationClient.createStorageLocationConfig(Mockito.any()))
               .thenReturn(new ResponseEntity<>(HateoasUtils.wrap(new StorageLocationConfigurationDto(1L,
                                                                                                      storageName,
                                                                                                      null,
                                                                                                      StorageType.ONLINE,
                                                                                                      0L,
                                                                                                      0L)),
                                                HttpStatusCode.valueOf(200)));
    }

    private void mockGet(String storageName) throws ModuleException {
        Mockito.when(storageLocationConfigurationClient.retrieveStorageLocationConfigByName(Mockito.anyString()))
               .thenReturn(new ResponseEntity<>(HateoasUtils.wrap(new StorageLocationConfigurationDto(1L,
                                                                                                      storageName,
                                                                                                      null,
                                                                                                      StorageType.ONLINE,
                                                                                                      0L,
                                                                                                      0L)),
                                                HttpStatusCode.valueOf(200)));

        Mockito.when(storageLocationRepository.findByName(storageName))
               .thenReturn(Optional.of(new StorageLocation(storageName)));
    }

    private void mockGetAll(String... storageName) throws ModuleException {
        List<StorageLocationConfigurationDto> confs = Arrays.stream(storageName)
                                                            .map(s -> new StorageLocationConfigurationDto(1L,
                                                                                                          s,
                                                                                                          null,
                                                                                                          StorageType.ONLINE,
                                                                                                          0L,
                                                                                                          0L))
                                                            .toList();
        Mockito.when(storageLocationConfigurationClient.retrieveAllStorageLocationConfigs())
               .thenReturn(new ResponseEntity<>(HateoasUtils.wrapList(confs), HttpStatusCode.valueOf(200)));

        Mockito.when(storageLocationRepository.findAll())
               .thenReturn(Arrays.stream(storageName).map(StorageLocation::new).toList());
    }

    private void mockUpdate(String storageName) throws ModuleException {
        Mockito.when(storageLocationConfigurationClient.updateStorageLocationConfigByName(Mockito.any(), Mockito.any()))
               .thenReturn(new ResponseEntity<>(HateoasUtils.wrap(new StorageLocationConfigurationDto(1L,
                                                                                                      storageName,
                                                                                                      null,
                                                                                                      StorageType.ONLINE,
                                                                                                      0L,
                                                                                                      10000L)),
                                                HttpStatusCode.valueOf(200)));
    }

    private void mockDelete() throws ModuleException {
        Mockito.when(storageLocationConfigurationClient.deleteStorageLocationConfigByName(Mockito.any()))
               .thenReturn(new ResponseEntity<>(HttpStatusCode.valueOf(200)));
    }

}
