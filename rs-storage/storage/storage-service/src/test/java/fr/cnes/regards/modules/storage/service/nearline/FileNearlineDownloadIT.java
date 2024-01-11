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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.storage.service.nearline;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoIT;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.service.PluginService;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.domain.DownloadableFile;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import fr.cnes.regards.modules.storage.domain.exception.NearlineDownloadException;
import fr.cnes.regards.modules.storage.domain.exception.NearlineFileNotAvailableException;
import fr.cnes.regards.modules.storage.service.file.FileDownloadService;
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

import static fr.cnes.regards.modules.storage.service.nearline.NearlineItUtils.EXPIRED_DATE;
import static fr.cnes.regards.modules.storage.service.nearline.NearlineItUtils.FILE_1;

/**
 * @author tguillou
 */
@ActiveProfiles({ "noscheduler", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_file_nearline_cache_tests" })
@ComponentScan(basePackages = { "fr.cnes.regards.modules.storage.service" }, lazyInit = true)
@EnableAutoConfiguration(exclude = { RabbitAutoConfiguration.class,
                                     AmqpAutoConfiguration.class,
                                     EurekaClientAutoConfiguration.class,
                                     EurekaDiscoveryClientConfiguration.class })
public class FileNearlineDownloadIT extends AbstractDaoIT {

    @Autowired
    private NearlineItUtils nearlineItUtils;

    @Autowired
    private FileDownloadService fileDownloadService;

    @Autowired
    protected ICacheFileRepository cacheFileRepository;

    @MockBean
    private PluginService pluginService;

    @Before
    public void init() throws ModuleException, NotAvailablePluginConfigurationException {
        cacheFileRepository.deleteAll();
        nearlineItUtils.initOnlineNearlineAndOfflineStoragesAndPlugins(pluginService);
    }

    @Test
    public void test_download_nearline_success() {
        // GIVEN a file in external cache
        CacheFile externalCachefile = nearlineItUtils.buildExternalCachefile(FILE_1);
        cacheFileRepository.save(externalCachefile);

        // WHEN trying to download this file
        DownloadableFile downloadableFile = null;
        try {
            downloadableFile = fileDownloadService.downloadFile(FILE_1).call();
        } catch (Exception e) {
            Assertions.fail("download should success here");
        }
        // THEN download don't fail
        Assertions.assertEquals(FILE_1, downloadableFile.getFileName());
        // THEN cache file is still present
        Assertions.assertTrue(cacheFileRepository.findOneByChecksum(FILE_1).isPresent());
    }

    @Test
    public void test_download_with_expired_file() throws Exception {
        // GIVEN an expired file in external cache
        CacheFile externalCachefile = nearlineItUtils.buildExternalCachefile(FILE_1);
        externalCachefile.setExpirationDate(EXPIRED_DATE);
        cacheFileRepository.save(externalCachefile);

        // WHEN trying to download this file
        try {
            fileDownloadService.downloadFile(FILE_1).call();
            Assertions.fail("download must fail because cache file is expired.");
        } catch (NearlineFileNotAvailableException e) {
            // THEN download fail
        }
        // THEN cache file has been removed from BD.
        Assertions.assertTrue(cacheFileRepository.findOneByChecksum(FILE_1).isEmpty());
    }

    @Test
    public void test_download_nearline_failed() throws Exception {
        // GIVEN a file in external cache
        CacheFile externalCachefile = nearlineItUtils.buildExternalCachefile(FILE_1);
        cacheFileRepository.save(externalCachefile);

        // force plugin download to throw a download exception (storage may be not available for example)
        Mockito.when(nearlineItUtils.getPluginStorageNearlineMockedInstance().download(Mockito.any()))
               .thenThrow(new NearlineDownloadException("download fail"));

        // WHEN trying to download this file
        try {
            fileDownloadService.downloadFile(FILE_1).call();
            Assertions.fail("download should failed here because plugin download failed");
        } catch (NearlineDownloadException e) {
            // THEN download fail
        }
        // THEN cache file is NOT removed because download exception does not mean that file is expired or not available.
        Assertions.assertTrue(cacheFileRepository.findOneByChecksum(FILE_1).isPresent());
    }

    @Test
    public void test_download_nearline_not_available() throws Exception {
        // GIVEN a file in external cache
        CacheFile externalCachefile = nearlineItUtils.buildExternalCachefile(FILE_1);
        cacheFileRepository.save(externalCachefile);
        // GIVEN force plugin download to throw a not available exception
        // (for sample file has been manually removed on external storage)
        Mockito.when(nearlineItUtils.getPluginStorageNearlineMockedInstance().download(Mockito.any()))
               .thenThrow(new NearlineFileNotAvailableException("file is not available anymore"));

        // WHEN trying to download this file
        try {
            fileDownloadService.downloadFile(FILE_1).call();
            Assertions.fail("download must fail because cache file is not available on external storage.");
        } catch (NearlineFileNotAvailableException e) {
            // THEN download fail
        }
        // THEN cache file has been removed from BD.
        Assertions.assertTrue(cacheFileRepository.findOneByChecksum(FILE_1).isEmpty());
    }
}
