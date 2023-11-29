/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.cache;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;
import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Test class for cache service.
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_cache_tests" },
                    locations = { "classpath:application-test.properties" })
public class CacheServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private ICacheFileRepository cacheFileRepository;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Autowired
    private IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    @Before
    public void init() throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        cacheFileRepository.deleteAll();
        dynamicTenantSettingRepository.deleteAll();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // we override cache setting values for tests
        dynamicTenantSettingService.update(StorageSetting.CACHE_MAX_SIZE_NAME, 5L);
    }

    @Test
    public void createCacheFile() throws MalformedURLException {
        // Given : initialize new file in cache
        String checksum = UUID.randomUUID().toString();
        int availabilityHours = 24;

        OffsetDateTime expirationDate = OffsetDateTime.now()
                                                      .plusHours(availabilityHours)
                                                      .atZoneSameInstant(ZoneOffset.UTC)
                                                      .toOffsetDateTime()
                                                      .truncatedTo(ChronoUnit.SECONDS);
        Assert.assertFalse("File should not referenced in cache", cacheService.getCacheFile(checksum).isPresent());

        // When
        cacheService.addFile(checksum,
                             123L,
                             "test.file.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             DataType.RAWDATA.name(),
                             new URL("file", null, "/plop/test.file.test"),
                             availabilityHours,
                             UUID.randomUUID().toString());
        Optional<CacheFile> cacheFileOptional = cacheService.getCacheFile(checksum);

        // Then
        Assert.assertTrue("File should be referenced in cache", cacheFileOptional.isPresent());
        Assert.assertTrue("Invalid expiration date truncated in second",
                          expirationDate.isEqual(cacheFileOptional.get()
                                                                  .getExpirationDate()
                                                                  .truncatedTo(ChronoUnit.SECONDS)));

        // Given : try to reference again the same file in cache
        availabilityHours = 2 * availabilityHours;
        OffsetDateTime newExpirationDate = OffsetDateTime.now()
                                                         .plusHours(availabilityHours)
                                                         .atZoneSameInstant(ZoneOffset.UTC)
                                                         .toOffsetDateTime()
                                                         .truncatedTo(ChronoUnit.SECONDS);

        // When
        cacheService.addFile(checksum,
                             123L,
                             "test.file.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             DataType.RAWDATA.name(),
                             new URL("file", null, "/plop/test.file.test"),
                             availabilityHours,
                             UUID.randomUUID().toString());
        cacheFileOptional = cacheService.getCacheFile(checksum);

        // Then
        Assert.assertTrue("File should be referenced in cache", cacheFileOptional.isPresent());
        Assert.assertTrue("Invalid expiration date truncated in second",
                          newExpirationDate.isEqual(cacheFileOptional.get()
                                                                     .getExpirationDate()
                                                                     .truncatedTo(ChronoUnit.SECONDS)));
    }

    @Test
    public void calculateCacheSize() throws MalformedURLException {
        // Given
        for (int i = 0; i < 10; i++) {
            cacheService.addFile(UUID.randomUUID().toString(),
                                 10L,
                                 "test.file.test",
                                 MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                 DataType.RAWDATA.name(),
                                 new URL("file", null, "/plop/test.file.test"),
                                 24,
                                 UUID.randomUUID().toString());
        }
        // When, then
        Assert.assertEquals("Total size not valid", 100L, cacheService.getCacheSizeUsedBytes().longValue());
    }

    /**
     * Test that cache is well purged when files are expired.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_450")
    @Purpose("Files in cache are purged when they are expired")
    public void purge() throws IOException {
        // Given
        OffsetDateTime expirationDate = OffsetDateTime.now().minusDays(25);
        // Create some files in cache
        List<CacheFile> cacheFiles = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            expirationDate = expirationDate.plusDays(1);

            cacheFiles.add(new CacheFile(UUID.randomUUID().toString(),
                                         10L,
                                         "test.file.test" + index,
                                         MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                         new URL("file", null, "/plop/test.file.test" + index),
                                         expirationDate,
                                         UUID.randomUUID().toString(),
                                         DataType.RAWDATA.name()));
        }
        cacheFileRepository.saveAll(cacheFiles);
        Assert.assertEquals("There should be 50 files in cache", 50, cacheFileRepository.findAll().size());

        // When: as we do not have create files on disk,
        // log of ERROR type : File to delete /plop/test.file.test<index> does not exists
        cacheService.purge(false);

        // Then
        Assert.assertEquals("There should be 25 files in cache", 25, cacheFileRepository.findAll().size());

        // When: as we do not have create files on disk, all files in cache are invalid and should deleted
        // log of WARN type : Dirty cache file in database : /plop/test.file.test<index>
        cacheService.checkDiskDBCoherence();
        //runtimeTenantResolver.forceTenant(getDefaultTenant());

        // Then
        Assert.assertEquals("There should be 0 files in cache", 0, cacheFileRepository.findAll().size());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_ARC_450")
    @Purpose("Files in cache are purged when they are expired")
    public void test_force_purge() throws IOException {
        // Given
        OffsetDateTime expirationDate = OffsetDateTime.now().minusDays(25);
        // Create some files in cache
        List<CacheFile> cacheFiles = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            expirationDate = expirationDate.plusDays(1);

            cacheFiles.add(new CacheFile(UUID.randomUUID().toString(),
                                         10L,
                                         "test.file.test" + index,
                                         MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                         new URL("file", null, "/plop/test.file.test" + index),
                                         expirationDate,
                                         UUID.randomUUID().toString(),
                                         DataType.RAWDATA.name()));
        }
        cacheFileRepository.saveAll(cacheFiles);
        Assert.assertEquals("There should be 1000 files in cache", 50, cacheFileRepository.findAll().size());

        // When: as we do not have create files on disk,
        // log of ERROR type : File to delete /plop/test.file.test<index> does not exists
        // Only 25 files are expired, nevertheless the force mode is activated so all files should deleted from the
        // cache
        cacheService.purge(true);

        // Then
        Assert.assertEquals("There should be 0 files in cache", 0, cacheFileRepository.findAll().size());
    }

    @Test
    public void checkCacheCoherence() throws IOException {
        // Given
        int nbFiles = 10;
        // Initilaize files in cache does not exist on disk
        List<CacheFile> files = Lists.newArrayList();
        for (int index = 0; index < nbFiles; index++) {
            files.add(new CacheFile(UUID.randomUUID().toString(),
                                    12L,
                                    "plip" + index + ".test",
                                    MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE),
                                    new URL("file:/plop/plip_" + index + ".test"),
                                    OffsetDateTime.now().plusDays(1),
                                    UUID.randomUUID().toString(),
                                    DataType.RAWDATA.name()));
        }
        cacheFileRepository.saveAll(files);

        Assert.assertEquals(nbFiles, cacheFileRepository.count());
        // Initialize a existing file on disk and in cache
        Path path = Paths.get(cacheService.getTenantCachePath().toString(), "example-one.txt");
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        String checksum = UUID.randomUUID().toString();
        try {
            cacheFileRepository.save(new CacheFile(checksum,
                                                   12L,
                                                   path.getFileName().toString(),
                                                   MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE),
                                                   new URL("file:" + path.toAbsolutePath()),
                                                   OffsetDateTime.now().plusDays(1),
                                                   UUID.randomUUID().toString(),
                                                   DataType.RAWDATA.name()));
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(nbFiles + 1, cacheFileRepository.count());

        // When
        // log of WARN type : Dirty cache file in database : /plop/plip_<index>.test
        cacheService.checkDiskDBCoherence();

        // Then
        Assert.assertEquals("File in database that does not exists on disk should be removed",
                            1,
                            cacheFileRepository.count());
        Optional<CacheFile> cacheFileOptionnal = cacheFileRepository.findOneByChecksum(checksum);
        Assert.assertTrue(cacheFileOptionnal.isPresent());
        Assert.assertEquals("example-one.txt", cacheFileOptionnal.get().getFileName());
    }
}
