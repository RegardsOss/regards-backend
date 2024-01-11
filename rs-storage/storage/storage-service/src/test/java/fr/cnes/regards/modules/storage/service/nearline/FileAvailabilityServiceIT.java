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
package fr.cnes.regards.modules.storage.service.nearline;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoIT;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.filecatalog.dto.availability.FileAvailabilityStatusDto;
import fr.cnes.regards.modules.filecatalog.dto.availability.FilesAvailabilityRequestDto;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IStorageLocationConfigurationRepostory;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.repository.IDownloadQuotaRepository;
import fr.cnes.regards.modules.storage.service.availability.FileAvailabilityService;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.netflix.eureka.EurekaClientAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClientConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;

import static fr.cnes.regards.modules.storage.service.nearline.NearlineItUtils.*;

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
    private NearlineItUtils nearlineItUtils;

    @Autowired
    private IStorageLocationConfigurationRepostory storageLocationConfigurationRepository;

    @MockBean
    private PluginService pluginService;

    @MockBean
    private IDownloadQuotaRepository mockedRepo;

    @Before
    public void init() throws ModuleException, NotAvailablePluginConfigurationException {
        cacheFileRepository.deleteAll();
        fileReferenceRepository.deleteAll();
        storageLocationConfigurationRepository.deleteAll();
        nearlineItUtils.initOnlineNearlineAndOfflineStoragesAndPlugins(pluginService);
    }

    @Test
    public void test_availability_response() throws EntityInvalidException {
        // GIVEN a file cached
        nearlineItUtils.storeCacheFiles(FILE_1);
        // WHEN requesting availability of this file
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_1, FILE_2));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);

        // THEN
        Assertions.assertEquals(1, availabilityResult.size()); // not existing file is not returned (FILE_2)
        FileAvailabilityStatusDto result = availabilityResult.get(0);
        Assertions.assertEquals(FILE_1, result.getChecksum()); // checksum and name are the same in these tests
        Assertions.assertTrue(result.isAvailable());
        Assertions.assertNotNull(result.getExpirationDate());

        nearlineItUtils.storeT2Files(FILE_2);
        // WHEN requesting availability of these files
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN files are available, and both have expiration date
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
        // make sure that expiration date is set for cached file and T2 file
        Assertions.assertTrue(availabilityResult.stream().allMatch(file -> file.getExpirationDate() != null));
    }

    @Test
    public void test_offline_to_cache() throws EntityInvalidException {
        // GIVEN files offline
        nearlineItUtils.storeOfflineFiles(FILE_1);
        nearlineItUtils.storeOfflineFiles(FILE_2);
        // WHEN requesting availability of these files
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_1, FILE_2));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN none are available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));

        // GIVEN now store these files in cache (don't care about extern or intern cache here)
        nearlineItUtils.storeCacheFiles(FILE_1);
        nearlineItUtils.storeCacheFiles(FILE_2);
        // WHEN requesting again availability of these files
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN all files are now available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
    }

    @Test
    public void test_offline_to_online() throws EntityInvalidException {
        // GIVEN files offline
        nearlineItUtils.storeOfflineFiles(FILE_1);
        nearlineItUtils.storeOfflineFiles(FILE_2);
        // WHEN requesting availability of these files
        FilesAvailabilityRequestDto request = new FilesAvailabilityRequestDto(Set.of(FILE_1, FILE_2));
        List<FileAvailabilityStatusDto> availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN none are available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));

        // GIVEN now store these files in another storage, which is online
        nearlineItUtils.storeOnlineFiles(FILE_1);
        nearlineItUtils.storeOnlineFiles(FILE_2);
        // WHEN requesting again availability of these files
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN all files are now available
        Assertions.assertEquals(2, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().allMatch(FileAvailabilityStatusDto::isAvailable));
    }

    @Test
    public void test_nearline_confirmed() throws EntityInvalidException {
        // GIVEN a file stored in T3
        nearlineItUtils.storeT3Files(FILE_T3);
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
        Assertions.assertEquals(1,
                                nearlineItUtils.getPluginStorageNearlineMockedInstance()
                                               .getCheckAvailabilityCallNumber());

        // WHEN requesting again availability of this file
        availabilityResult = fileAvailabilityService.checkFileAvailability(request);
        // THEN file is not available (but the plugin has not been called)
        Assertions.assertEquals(1, availabilityResult.size());
        Assertions.assertTrue(availabilityResult.stream().noneMatch(FileAvailabilityStatusDto::isAvailable));
        // THEN verify that plugin has been called 1 times (again)
        Assertions.assertEquals(1,
                                nearlineItUtils.getPluginStorageNearlineMockedInstance()
                                               .getCheckAvailabilityCallNumber());
    }

    @Test
    public void test_nearline_T2_to_T3() throws EntityInvalidException {
        // GIVEN a file stored in T2
        nearlineItUtils.storeT2Files(FILE_NEARLINE);
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
        nearlineItUtils.simulateFilePassT2toT3(fileNearline);

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
        Assertions.assertEquals(2,
                                nearlineItUtils.getPluginStorageNearlineMockedInstance()
                                               .getCheckAvailabilityCallNumber());
    }
}
