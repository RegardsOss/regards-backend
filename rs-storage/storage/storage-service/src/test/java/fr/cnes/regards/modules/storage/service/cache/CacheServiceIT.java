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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.modules.tenant.settings.dao.IDynamicTenantSettingRepository;
import fr.cnes.regards.framework.modules.tenant.settings.service.AbstractSettingService;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.modules.storage.dao.ICacheFileRepository;
import fr.cnes.regards.modules.storage.domain.StorageSetting;
import fr.cnes.regards.modules.storage.domain.database.CacheFile;

/**
 * Test class for cache service.
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=storage_cache_tests" },
        locations = { "classpath:application-test.properties" })
public class CacheServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    private CacheService service;

    @Autowired
    private ICacheFileRepository repository;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Autowired
    private IDynamicTenantSettingRepository dynamicTenantSettingRepository;

    @Before
    public void init() throws EntityNotFoundException, EntityOperationForbiddenException, EntityInvalidException {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        repository.deleteAll();
        dynamicTenantSettingRepository.deleteAll();
        simulateApplicationStartedEvent();
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // we override cache setting values for tests
        dynamicTenantSettingService.update(StorageSetting.CACHE_MAX_SIZE_NAME, 5L);
    }

    @Test
    public void createCacheFile() throws MalformedURLException {
        // Initialize new file in cache
        String checksum = UUID.randomUUID().toString();
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(1);
        Assert.assertFalse("File should not referenced in cache", service.getCacheFile(checksum).isPresent());
        service.addFile(checksum, 123L, "test.file.test", MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                        DataType.RAWDATA.name(), new URL("file", null, "/plop/test.file.test"), expirationDate,
                        UUID.randomUUID().toString());
        Optional<CacheFile> oCf = service.getCacheFile(checksum);
        Assert.assertTrue("File should be referenced in cache", oCf.isPresent());
        Assert.assertTrue("Invalid expiration date", expirationDate.isEqual(oCf.get().getExpirationDate()));
        // Try to reference again the same file in cache
        OffsetDateTime newExpirationDate = OffsetDateTime.now().plusDays(2);
        service.addFile(checksum, 123L, "test.file.test", MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE),
                        DataType.RAWDATA.name(), new URL("file", null, "/plop/test.file.test"), newExpirationDate,
                        UUID.randomUUID().toString());
        oCf = service.getCacheFile(checksum);
        Assert.assertTrue("File should be referenced in cache", oCf.isPresent());
        Assert.assertTrue("Invalid expiration date", newExpirationDate.isEqual(oCf.get().getExpirationDate()));
    }

    @Test
    public void calculateCacheSize() throws MalformedURLException {
        OffsetDateTime expirationDate = OffsetDateTime.now().plusDays(1);
        for (int i = 0; i < 1_000; i++) {
            service.addFile(UUID.randomUUID().toString(), 10L, "test.file.test",
                            MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE), DataType.RAWDATA.name(),
                            new URL("file", null, "/plop/test.file.test"), expirationDate,
                            UUID.randomUUID().toString());
        }
        Assert.assertEquals("Total size not valid", 10_000L, service.getCacheSizeUsedBytes().longValue());
    }

    /**
     * Test that cache is well purged when files are expired.
     */
    @Test
    @Requirement("REGARDS_DSL_STO_ARC_450")
    @Purpose("Files in cache are purged when they are expired")
    public void purge() throws IOException {
        OffsetDateTime expirationDate = OffsetDateTime.now().minusDays(100);
        // Create some files in cache
        for (int i = 0; i < 1_000; i++) {
            expirationDate = expirationDate.plusDays(1);
            service.addFile(UUID.randomUUID().toString(), 10L, "test.file.test",
                            MimeType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE), DataType.RAWDATA.name(),
                            new URL("file", null, "/plop/test.file.test"), expirationDate,
                            UUID.randomUUID().toString());
        }
        Assert.assertEquals("There should be 1000 files in cache", 1000, repository.findAll().size());
        service.purge();
        Assert.assertEquals("There should be 900 files in cache", 900, repository.findAll().size());
        // As we do not have create files on disk, all files in cache are invalid and should deleted
        service.checkDiskDBCoherence();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        Assert.assertEquals("There should be 0 files in cache", 0, repository.findAll().size());
    }

    @Test
    public void checkCacheCoherence() throws IOException {

        int nbFiles = 50;
        // Init files in cache does not exists
        List<CacheFile> files = Lists.newArrayList();
        for (int i = 0; i < nbFiles; i++) {
            files.add(new CacheFile(UUID.randomUUID().toString(), 12L, "plip" + i + ".test",
                    MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE), new URL("file:/plop/plip_" + i + ".test"),
                    OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString(), "RAWDATA"));
        }
        repository.saveAll(files);
        // Init existing files in cache
        Path path = Paths.get(service.getTenantCachePath().toString(), "example-one.txt");
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
        Files.walk(path).filter(Files::isRegularFile).forEach(p -> {
            try {
                repository.save(new CacheFile(UUID.randomUUID().toString(), 12L, p.getFileName().toString(),
                                              MimeType.valueOf(MediaType.APPLICATION_ATOM_XML_VALUE), new URL("file:" + p.toAbsolutePath().toString()),
                        OffsetDateTime.now().plusDays(1), UUID.randomUUID().toString(), "RAWDATA"));
            } catch (MalformedURLException e) {
                Assert.fail(e.getMessage());
            }
        });

        Assert.assertEquals(nbFiles + 1, repository.count());
        service.checkDiskDBCoherence();
        Assert.assertEquals("File in database that does not exists on disk should be removed", 1, repository.count());

    }
}
