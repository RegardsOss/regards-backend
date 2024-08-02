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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
        // we override cache setting values for tests(5 Kilo-octets)
        dynamicTenantSettingService.update(StorageSetting.CACHE_MAX_SIZE_NAME, 5L);
    }

    @Test
    public void create_cacheFile_in_internalCache() throws MalformedURLException {
        // Given : initialize new file in cache
        String checksum = UUID.randomUUID().toString();
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MICROS);

        Assert.assertFalse("File should not referenced in cache", cacheService.findByChecksum(checksum).isPresent());

        // When
        cacheService.addFile(checksum,
                             123L,
                             "test.file.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             DataType.RAWDATA.name(),
                             new URL("file", null, "/plop/test.file.test"),
                             expirationDate,
                             Set.of(UUID.randomUUID().toString()),
                             null);
        Optional<CacheFile> cacheFileOptional = cacheService.findByChecksum(checksum);

        // Then
        Assert.assertTrue("File should be referenced in cache", cacheFileOptional.isPresent());
        Assert.assertTrue("File should be in internal cache", cacheFileOptional.get().isInternalCache());
        Assert.assertNull("Businness identifier of plugin must be null because internal cache",
                          cacheFileOptional.get().getExternalCachePlugin());
        Assert.assertTrue("Invalid expiration date",
                          expirationDate.isEqual(cacheFileOptional.get().getExpirationDate()));

        // Given : try to reference again the same file in cache
        OffsetDateTime newExpirationDate = OffsetDateTime.now().plusDays(2).truncatedTo(ChronoUnit.MICROS);

        // When
        cacheService.addFile(checksum,
                             123L,
                             "test.file.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             DataType.RAWDATA.name(),
                             new URL("file", null, "/plop/test.file.test"),
                             newExpirationDate,
                             Set.of(UUID.randomUUID().toString()),
                             null);
        cacheFileOptional = cacheService.findByChecksum(checksum);

        // Then
        Assert.assertTrue("File should be referenced in cache", cacheFileOptional.isPresent());
        Assert.assertTrue("Invalid expiration date",
                          newExpirationDate.isEqual(cacheFileOptional.get().getExpirationDate()));
    }

    @Test
    public void create_cacheFile_in_externalCache() throws MalformedURLException {
        // Given : initialize new file in cache
        String checksum = UUID.randomUUID().toString();
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(1).truncatedTo(ChronoUnit.MICROS);

        Assert.assertFalse("File should not referenced in cache", cacheService.findByChecksum(checksum).isPresent());

        // When
        cacheService.addFile(checksum,
                             123L,
                             "test.file.test",
                             MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                             DataType.RAWDATA.name(),
                             new URL("http", null, "/plop/test.file.test"),
                             expirationDate,
                             Set.of(UUID.randomUUID().toString()),
                             "business_identifier_plugin");
        Optional<CacheFile> cacheFileOptional = cacheService.findByChecksum(checksum);

        // Then
        Assert.assertTrue("File should be referenced in cache", cacheFileOptional.isPresent());
        Assert.assertFalse("File should be in external cache", cacheFileOptional.get().isInternalCache());
        Assert.assertEquals("Businness identifier of plugin must exist because external cache",
                            "business_identifier_plugin",
                            cacheFileOptional.get().getExternalCachePlugin());
        Assert.assertTrue("Invalid expiration date",
                          expirationDate.isEqual(cacheFileOptional.get().getExpirationDate()));
    }

    @Test
    public void test_compute_size_internalCache() throws MalformedURLException {
        // Given
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(1);
        for (int i = 0; i < 10; i++) {
            cacheService.addFile(UUID.randomUUID().toString(),
                                 10L,
                                 "test.file.test",
                                 MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                                 DataType.RAWDATA.name(),
                                 new URL("file", null, "/plop/test.file.test"),
                                 expirationDate,
                                 Set.of(UUID.randomUUID().toString()),
                                 null);
        }
        // When, then
        Assert.assertEquals("Total size not valid", 100L, cacheService.getCacheSizeUsedBytes().longValue());
    }

    @Test
    public void test_compute_size_internalCache_with_realFile() throws IOException {
        // Given
        String fileName = "fileTest";
        Path pathFileTest = Paths.get("src/test/resources/input").resolve(fileName);

        // Clean files in internal cache
        Files.walk(cacheService.getTenantCachePath())
             .filter(Files::isRegularFile)
             .map(Path::toFile)
             .forEach(File::delete);

        // When
        Files.copy(pathFileTest, cacheService.getTenantCachePath().resolve(fileName));
        cacheFileRepository.save(CacheFile.buildFileInternalCache(UUID.randomUUID().toString(),
                                                                  pathFileTest.toFile().length(),
                                                                  pathFileTest.getFileName().toString(),
                                                                  MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE),
                                                                  new URL("file", null, pathFileTest.toString()),
                                                                  OffsetDateTime.now().plusDays(1),
                                                                  Set.of(UUID.randomUUID().toString()),
                                                                  DataType.RAWDATA.name()));

        // Then
        Assert.assertEquals(5120L, cacheService.getMaxCacheSizeBytes().longValue());
        Assert.assertEquals(0L, cacheService.getCacheSizeUsedKBytes().longValue());
        Assert.assertEquals(cacheService.getMaxCacheSizeBytes().intValue() - pathFileTest.toFile().length(),
                            cacheService.getFreeSpaceInBytes().longValue());
        Assert.assertEquals(pathFileTest.toFile().length(), cacheService.getCacheSizeUsedBytes().longValue());
    }

    /**
     * Test that internal cache is well purged when files are expired.
     * Test force mode= false.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_450")
    @Purpose("Files in internal cache are purged when they are expired")
    public void test_purge_expiredFiles_internalCache() throws IOException {
        // Given
        OffsetDateTime expirationDate = OffsetDateTime.now().minusDays(25);
        // Create some files in internal cache
        List<CacheFile> cacheFiles = new ArrayList<>();
        for (int index = 0; index < 50; index++) {
            expirationDate = expirationDate.plusDays(1);
            // Expired files and not expired files ininternal cache
            cacheFiles.add(createFakeInternalCacheFile(index, expirationDate));
        }
        cacheFileRepository.saveAll(cacheFiles);
        Assert.assertEquals("There should be 50 files in internal cache", 50, cacheFileRepository.findAll().size());

        // When: as we do not have create files on disk,
        // log of ERROR type : File to delete /plop/test.file.test<index> does not exists
        cacheService.purge(false);

        // Then
        Assert.assertEquals("There should be 25 files in internal cache", 25, cacheFileRepository.findAll().size());

        // When: as we do not have create files on disk, all files in cache are invalid and should deleted
        // log of WARN type : Dirty cache file in database : /plop/test.file.test<index>
        cacheService.checkDiskDBCoherence();

        // Then
        Assert.assertEquals("There should be 0 files in internal cache", 0, cacheFileRepository.findAll().size());
        Assert.assertTrue(cacheService.isCacheEmpty());
    }

    /**
     * Test that internal cache is well purged when files are expired.
     * Test that external cache is not purged.
     * Test force mode= false.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_450")
    @Purpose("Files in internal cache are purged when they are expired")
    public void test_purge_expiredFiles_internal_external_cache() throws IOException {
        // Given
        List<CacheFile> cacheFiles = new ArrayList<>();
        // Create some files in internal cache
        OffsetDateTime expirationDateInternalCache = OffsetDateTime.now().minusDays(5);
        for (int index = 0; index < 10; index++) {
            expirationDateInternalCache = expirationDateInternalCache.plusDays(1);
            // Expired files and not expired files internal cache
            cacheFiles.add(createFakeInternalCacheFile(index, expirationDateInternalCache));
        }
        // Create some files in external cache
        OffsetDateTime expirationDateExternalCache = OffsetDateTime.now().minusDays(5);
        for (int index = 0; index < 10; index++) {
            expirationDateExternalCache = expirationDateExternalCache.plusDays(1);
            // Expired files and not expired files in external cache
            cacheFiles.add(createFakeExternalCacheFile(index, expirationDateExternalCache));
        }
        cacheFileRepository.saveAll(cacheFiles);
        Assert.assertEquals("There should be 20 files in internal/external cache",
                            20,
                            cacheFileRepository.findAll().size());

        // When: as we do not have create files on disk,
        // log of ERROR type : File to delete /plop/test.file.test<index> does not exists
        cacheService.purge(false);

        // Then
        Assert.assertEquals("There should be 15 files in internal/external cache",
                            15,
                            cacheFileRepository.findAll().size());
        // 5 files in internal caches, so 10 files in external cache
        Assert.assertEquals("There should be 5 files in internal cache",
                            5,
                            cacheFileRepository.countCacheFileByInternalCacheTrue());

        // When: as we must not create files on disk, all files in cache are invalid and should deleted
        // log of WARN type : Dirty cache file in database : /plop/test.file.test<index>
        cacheService.checkDiskDBCoherence();

        // Then
        Assert.assertEquals("There should be 10 files in external cache", 10, cacheFileRepository.findAll().size());
        Assert.assertTrue(cacheService.isCacheEmpty());
    }

    /**
     * Test that internal/external cache is well purged.
     * Test force mode= true.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_450")
    @Purpose("Files in cache are purged when they are expired")
    public void test_force_purge() throws IOException {
        // Given
        List<CacheFile> cacheFiles = new ArrayList<>();
        // Create some files in internal cache
        OffsetDateTime expirationDateInternalCache = OffsetDateTime.now().minusDays(5);
        for (int index = 0; index < 10; index++) {
            expirationDateInternalCache = expirationDateInternalCache.plusDays(1);
            // Expired files and not expired files internal cache
            cacheFiles.add(createFakeInternalCacheFile(index, expirationDateInternalCache));
        }
        // Create some files in external cache
        OffsetDateTime expirationDateExternalCache = OffsetDateTime.now().minusDays(5);
        for (int index = 0; index < 10; index++) {
            expirationDateExternalCache = expirationDateExternalCache.plusDays(1);
            // Expired files and not expired files in external cache
            cacheFiles.add(createFakeExternalCacheFile(index, expirationDateExternalCache));
        }
        cacheFileRepository.saveAll(cacheFiles);
        Assert.assertEquals("There should be 20 files in internal/external cache",
                            20,
                            cacheFileRepository.findAll().size());

        // When: as we must not create files on disk, log of ERROR type :
        // File to delete /plop/test.file.internal<index> and /plop/test.file.external<index> does not exists
        // Only 5 files are expired in internal cache
        // Only 5 files are expired in external cache
        // Nevertheless the force mode is activated so all files should deleted from the internal/external cache
        cacheService.purge(true);

        // Then
        Assert.assertEquals("There should be 0 files in internal/external cache",
                            0,
                            cacheFileRepository.findAll().size());
        Assert.assertTrue(cacheService.isCacheEmpty());
    }

    @Test
    public void test_checkCoherence_internalCache() throws IOException {
        // Given
        int nbFiles = 10;
        // Initilaize files in internal cache, files do not exist on disk
        List<CacheFile> files = new ArrayList<>();
        for (int index = 0; index < nbFiles; index++) {
            files.add(createFakeInternalCacheFile(index, OffsetDateTime.now().plusDays(1)));
        }
        cacheFileRepository.saveAll(files);

        Assert.assertEquals(nbFiles, cacheFileRepository.count());
        // Initialize a existing file on disk and in internal cache
        Path path = Paths.get(cacheService.getTenantCachePath().toString(), "example-one.txt");
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        String checksum = UUID.randomUUID().toString();
        try {
            cacheFileRepository.save(CacheFile.buildFileInternalCache(checksum,
                                                                      12L,
                                                                      path.getFileName().toString(),
                                                                      MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE),
                                                                      new URL("file:" + path.toAbsolutePath()),
                                                                      OffsetDateTime.now().plusDays(1),
                                                                      Set.of(UUID.randomUUID().toString()),
                                                                      DataType.RAWDATA.name()));
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertEquals(nbFiles + 1, cacheFileRepository.count());

        // When
        // log of WARN type : Dirty cache file in database : /plop/plip_<index>.test
        cacheService.checkDiskDBCoherence();

        // Then
        Assert.assertEquals("Files in database that do not exist on disk should be removed",
                            1,
                            cacheFileRepository.count());
        Optional<CacheFile> cacheFileOptionnal = cacheFileRepository.findOneByChecksum(checksum);
        Assert.assertTrue(cacheFileOptionnal.isPresent());
        Assert.assertEquals("example-one.txt", cacheFileOptionnal.get().getFileName());
    }

    @Test
    public void test_checkCoherence_externalCache() throws IOException {
        // Given
        int nbFiles = 10;
        // Initilaize files in external cache, files do not exist on disk
        List<CacheFile> files = new ArrayList<>();
        for (int index = 0; index < nbFiles; index++) {
            files.add(createFakeExternalCacheFile(index, OffsetDateTime.now().plusDays(1)));
        }
        cacheFileRepository.saveAll(files);

        Assert.assertEquals(nbFiles, cacheFileRepository.count());

        // When
        // log of WARN type : Dirty cache file in database : /plop/plip_<index>.test
        cacheService.checkDiskDBCoherence();

        // Then
        Assert.assertEquals("Files in database that do not exist on disk should not be removed",
                            nbFiles,
                            cacheFileRepository.count());
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private CacheFile createFakeInternalCacheFile(int index, OffsetDateTime expirationDate)
        throws MalformedURLException {
        return CacheFile.buildFileInternalCache(UUID.randomUUID().toString(),
                                                10L,
                                                "file" + index + ".test.internal",
                                                MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE),
                                                new URL("file", null, "/plop" + "/test.file.internal" + index),
                                                expirationDate,
                                                Set.of(UUID.randomUUID().toString()),
                                                DataType.RAWDATA.name());
    }

    private CacheFile createFakeExternalCacheFile(int index, OffsetDateTime expirationDate)
        throws MalformedURLException {
        return CacheFile.buildFileExternalCache(UUID.randomUUID().toString(),
                                                10L,
                                                "file" + index + ".test.external",
                                                MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE),
                                                new URL("file", null, "/plop/test.file.external" + index),
                                                expirationDate,
                                                Set.of(UUID.randomUUID().toString()),
                                                DataType.RAWDATA.name(),
                                                "Businness identifier of plugin");
    }
}
