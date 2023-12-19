/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.availability;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.dao.IPluginConfigurationRepository;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.filecatalog.dto.StorageType;
import fr.cnes.regards.modules.filecatalog.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.filecatalog.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IStorageLocationConfigurationRepostory;
import fr.cnes.regards.modules.storage.domain.database.*;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas GUILLOU
 **/
@ActiveProfiles({ "noscheduler", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_file_availability_tests" })
@ComponentScan(basePackages = { "fr.cnes.regards.modules.storage.service" }, lazyInit = true)
@EnableAutoConfiguration(exclude = { RabbitAutoConfiguration.class,
                                     AmqpAutoConfiguration.class,
                                     EurekaClientAutoConfiguration.class,
                                     EurekaDiscoveryClientConfiguration.class })
public class FileAvailabilityServiceIT extends AbstractDaoIT {

    @Autowired
    private FileAvailabilityService fileAvailabilityService;

    @Autowired
    private IFileReferenceRepository fileReferenceRepository;

    @Autowired
    protected ICacheFileRepository cacheFileRepository;

    @Autowired
    private IStorageLocationConfigurationRepostory storageLocationConfigurationRepository;

    @Autowired
    private IPluginConfigurationRepository pluginConfigurationRepository;

    @MockBean
    private PluginService pluginService;

    @MockBean
    private IDownloadQuotaRepository mockedRepo;

    private static final String FILE_1 = "FILE_CACHED_1";

    private static final String FILE_2 = "FILE_CACHED_2";

    private static final String FILE_T3 = "FILE_T3";

    private static final String FILE_NEARLINE = "FILE_NEARLINE";

    private StorageNearlineMocked pluginStorageNearlineMockedInstance;

    @Before
    public void init() throws ModuleException, NotAvailablePluginConfigurationException {
        cacheFileRepository.deleteAll();
        fileReferenceRepository.deleteAll();
        storageLocationConfigurationRepository.deleteAll();
        initStoragesAndPlugins();
        HttpURLConnection httpURLConnection = new HttpURLConnection(null) {

            @Override
            public void connect() throws IOException {

            }

            @Override
            public void disconnect() {

            }

            @Override
            public boolean usingProxy() {
                return false;
            }
        };

    }

    private void initStoragesAndPlugins() throws ModuleException, NotAvailablePluginConfigurationException {
        PluginConfiguration pluginConfiguration = buildNearlineConfiguration();
        pluginConfigurationRepository.save(pluginConfiguration);
        List<StorageLocationConfiguration> storages = List.of(buildStorage("Online"),
                                                              buildStorage("Nearline", pluginConfiguration),
                                                              buildStorage("Offline"));
        storageLocationConfigurationRepository.saveAll(storages);
        pluginStorageNearlineMockedInstance = new StorageNearlineMocked();
        Mockito.when(pluginService.getPlugin(Mockito.anyString())).thenReturn(pluginStorageNearlineMockedInstance);
    }

    @Test
    public void test_availability_response() {
        // GIVEN a file cached
        storeCacheFiles(FILE_1);
        // WHEN requesting availability of this file
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_1, FILE_2));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);

        // THEN
        Assertions.assertEquals(1, availabilityResult.size()); // not existing file is not returned (FILE_2)
        FileAvailabilityStatusDto result = availabilityResult.get(0);
        Assertions.assertEquals(FILE_1, result.getChecksum()); // checksum and name are the same in these tests
        Assertions.assertTrue(result.isAvailable());
        Assertions.assertNotNull(result.getExpirationDate());

        storeT2Files(FILE_2);
        // WHEN requesting availability of these files
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN files are available, and both have expiration date
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
        // make sure that expiration date is set for cached file and T2 file
        Assertions.assertTrue(availabilityResult.stream().allMatch(file -> file.getExpirationDate() != null));
    }

    @Test
    public void test_offline_to_cache() {
        // GIVEN files offline
        storeOfflineFiles(FILE_1);
        storeOfflineFiles(FILE_2);
        // WHEN requesting availability of these files
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_1, FILE_2));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN none are available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));

        // GIVEN now store these files in cache (don't care about extern or intern cache here)
        storeCacheFiles(FILE_1);
        storeCacheFiles(FILE_2);
        // WHEN requesting again availability of these files
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN all files are now available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
    }

    @Test
    public void test_offline_to_online() {
        // GIVEN files offline
        storeOfflineFiles(FILE_1);
        storeOfflineFiles(FILE_2);
        // WHEN requesting availability of these files
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_1, FILE_2));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN none are available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));

        // GIVEN now store these files in another storage, which is online
        storeOnlineFiles(FILE_1);
        storeOnlineFiles(FILE_2);
        // WHEN requesting again availability of these files
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN all files are now available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
    }

    @Test
    public void test_nearline_confirmed() throws ModuleException, NotAvailablePluginConfigurationException {
        // GIVEN a file stored in T3
        storeT3Files(FILE_T3);
        // WHEN requesting availability of this file
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_T3));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN file is not available (call of plugin S3 return not available)
        Assertions.assertEquals(1, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));
        FileReference fileRefT3 = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksum("Nearline", FILE_T3)
                                                         .get();
        // THEN verify nearline confirmed is well modified
        Assertions.assertTrue(fileRefT3.isNearlineConfirmed());
        // THEN verify that plugin has been called 1 times
        Assertions.assertEquals(1, pluginStorageNearlineMockedInstance.getCheckAvailabilityCallNumber());

        // WHEN requesting again availability of this file
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN file is not available (but the plugin has not been called)
        Assertions.assertEquals(1, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));
        // THEN verify that plugin has been called 1 times (again)
        Assertions.assertEquals(1, pluginStorageNearlineMockedInstance.getCheckAvailabilityCallNumber());
    }

    @Test
    public void test_nearline_T2_to_T3() throws ModuleException, NotAvailablePluginConfigurationException {
        // GIVEN a file stored in T2
        storeT2Files(FILE_NEARLINE);
        // WHEN requesting availability of these files
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_NEARLINE));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN file is available (call of plugin S3 return available)
        Assertions.assertEquals(1, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
        FileReference fileNearline = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksum("Nearline",
                                                                                                      FILE_NEARLINE)
                                                            .get();
        // THEN verify nearline confirmed is still false
        Assertions.assertFalse(fileNearline.isNearlineConfirmed());

        // GIVEN file is not stored in T2 but in T3
        simulateFilePassT2toT3(fileNearline);

        // WHEN requesting availability of this file
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN file is NOT available (call of plugin S3 return NOT available)
        Assertions.assertEquals(1, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));
        fileNearline = fileReferenceRepository.findByLocationStorageAndMetaInfoChecksum("Nearline", FILE_NEARLINE)
                                              .get();
        // THEN verify nearline confirmed is now passed to true
        Assertions.assertTrue(fileNearline.isNearlineConfirmed());

        // THEN verify that plugin has been called 2 times
        Assertions.assertEquals(2, pluginStorageNearlineMockedInstance.getCheckAvailabilityCallNumber());
    }

    private void simulateFilePassT2toT3(FileReference fileNearline) {
        fileNearline.getMetaInfo().setFileName(FILE_NEARLINE + StorageNearlineMocked.T3_PATTERN);
        fileReferenceRepository.save(fileNearline);
    }

    private void storeT3Files(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files)
                                              .map(file -> buildFileReference(file, "Nearline"))
                                              .toList());
    }

    private void storeT2Files(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files)
                                              .map(file -> buildFileReference(file, "Nearline"))
                                              .toList());
    }

    private void storeCacheFiles(String... files) {
        cacheFileRepository.saveAll(Arrays.stream(files).map(this::buildCachefile).toList());
    }

    private void storeOfflineFiles(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files).map(file -> buildFileReference(file, "Offline")).toList());
    }

    private void storeOnlineFiles(String... files) {
        fileReferenceRepository.saveAll(Arrays.stream(files).map(file -> buildFileReference(file, "Online")).toList());
    }

    private void initDatas() {
        List<CacheFile> cacheFiles = List.of(buildCachefile(FILE_1), buildCachefile(FILE_2));
        cacheFileRepository.saveAll(cacheFiles);

        List<FileReference> fileReferences = List.of(buildFileReference("FILE_1_T3", "Nearline"),
                                                     buildFileReference("FILE_2_T3", "Nearline"),
                                                     buildFileReference("FILE_3_T2", "Nearline"),
                                                     buildFileReference("FILE_4_T2", "Nearline"),
                                                     buildFileReference("FILE_5_Online", "Online"),
                                                     buildFileReference("FILE_6_Online", "Online"),
                                                     buildFileReference("FILE_7_Offline", "Offline"),
                                                     buildFileReference("FILE_8_Multi", "Online"),
                                                     buildFileReference("FILE_8_Multi", "Nearline"));
        fileReferenceRepository.saveAll(fileReferences);
        List<StorageLocationConfiguration> storages = List.of(buildStorage("Online"),
                                                              buildStorage("Nearline"),
                                                              buildStorage("Offline"));
        storageLocationConfigurationRepository.saveAll(storages);
    }

    private PluginConfiguration buildNearlineConfiguration() {
        //        PluginMetaData metaData = PluginUtils.createPluginMetaData(StorageNearlineMocked.class);
        PluginConfiguration configuration = new PluginConfiguration("Nearline",
                                                                    Set.of(),
                                                                    0,
                                                                    StorageNearlineMocked.PLUGIN_ID);
        configuration.setMetaData(new PluginMetaData());
        configuration.setVersion("1.0");
        configuration.setIsActive(true);
        return configuration;
    }

    private CacheFile buildCachefile(String name) {
        return new CacheFile(name,
                             10L,
                             name,
                             MimeType.valueOf("application/json"),
                             null,
                             OffsetDateTime.now().plusHours(1),
                             "groupId",
                             "type",
                             true,
                             null);
    }

    private StorageLocationConfiguration buildStorage(String name) {
        return buildStorage(name, null);
    }

    private StorageLocationConfiguration buildStorage(String name, PluginConfiguration pluginConfiguration) {
        StorageLocationConfiguration storageLocationConfiguration = new StorageLocationConfiguration(name,
                                                                                                     pluginConfiguration,
                                                                                                     50L);
        storageLocationConfiguration.setStorageType(StorageType.valueOf(name.toUpperCase()));
        return storageLocationConfiguration;
    }

    private FileReference buildFileReference(String name, String storage) {
        FileReference fileReference = new FileReference();
        fileReference.setMetaInfo(new FileReferenceMetaInfo(name,
                                                            "algorithm",
                                                            name,
                                                            50L,
                                                            MimeType.valueOf("application/json")));
        fileReference.setLocation(new FileLocation(storage, storage, false));
        return fileReference;
    }

}
