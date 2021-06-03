/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.service.location;

import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storage.dao.IGroupRequestInfoRepository;
import fr.cnes.regards.modules.storage.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.storage.dao.IStorageMonitoringRepository;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;
import fr.cnes.regards.modules.storage.domain.dto.StorageLocationDTO;
import fr.cnes.regards.modules.storage.domain.plugin.StorageType;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;
import fr.cnes.regards.modules.storage.service.file.request.FileReferenceRequestService;

/**
 * Test class
 *
 * @author Sébastien Binda
 *
 */
@ActiveProfiles("noschedule")
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache" }, locations = { "classpath:application-test.properties" })
public class StorageLocationServiceTest extends AbstractStorageTest {

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IStorageLocationRepository storageLocationRepo;

    @Autowired
    private IGroupRequestInfoRepository requInfoRepo;

    @Autowired
    private IStorageMonitoringRepository storageMonitorRepo;

    @Autowired
    private FileReferenceRequestService fileRefService;

    @Before
    public void initialize() throws ModuleException {
        requInfoRepo.deleteAll();
        fileRefRepo.deleteAll();
        storageLocationRepo.deleteAll();
        storageMonitorRepo.deleteAll();
        super.init();
    }

    private void createFileReference(String storage, Long fileSize) {
        String checksum = UUID.randomUUID().toString();
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", "file.test", fileSize,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation location = new FileLocation(storage, "anywhere://in/this/directory/" + checksum);
        try {
            fileRefService.reference("someone", fileMetaInfo, location, Sets.newHashSet(UUID.randomUUID().toString())
                    ,"source1", "session1");
        } catch (ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void monitorStorageLocation() {
        Long totalSize = 0L;
        String storage = "STAF";
        Assert.assertFalse("0. There not have file referenced yet", storageLocationService.search(storage).isPresent());
        storageLocationService.monitorStorageLocations(false);
        Assert.assertFalse("1. There not have file referenced yet", storageLocationService.search(storage).isPresent());
        createFileReference(storage, 1024L);
        totalSize++;
        createFileReference(storage, 1024L);
        totalSize++;
        createFileReference(storage, 1024L);
        totalSize++;
        createFileReference(storage, 1024L);
        totalSize++;
        storageLocationService.monitorStorageLocations(false);
        Assert.assertTrue("There should be file referenced for STAF storage",
                          storageLocationService.search(storage).isPresent());
        Assert.assertEquals("Total size on STAF storage is invalid", totalSize.longValue(), storageLocationService
                .search(storage).get().getTotalSizeOfReferencedFilesInKo().longValue());
        Assert.assertEquals("Total number of files on STAF storage is invalid", 4L,
                            storageLocationService.search(storage).get().getNumberOfReferencedFiles().longValue());
        createFileReference(storage, 3 * 1024L);
        totalSize += 3;
        storageLocationService.monitorStorageLocations(false);
        Assert.assertTrue("There should be file referenced for STAF storage",
                          storageLocationService.search(storage).isPresent());
        Assert.assertEquals("Total size on STAF storage is invalid", totalSize.longValue(), storageLocationService
                .search(storage).get().getTotalSizeOfReferencedFilesInKo().longValue());
        Assert.assertEquals("Total number of files on STAF storage invalid", 5L,
                            storageLocationService.search(storage).get().getNumberOfReferencedFiles().longValue());
    }

    @Test
    public void retrieveOne() throws ModuleException {
        String storage = "STAF";
        createFileReference(storage, 2048L);
        createFileReference(storage, 2048L);
        storageLocationService.monitorStorageLocations(false);
        StorageLocationDTO loc = storageLocationService.getById(storage);
        Assert.assertNotNull("A location should be retrieved", loc);
        Assert.assertNull("No configuration should be set for the location", loc.getConfiguration());
        Assert.assertEquals("There should be 2 files referenced", 2L, loc.getNbFilesStored().longValue());
        Assert.assertEquals("The total size should be 4ko", 4L, loc.getTotalStoredFilesSizeKo().longValue());
        Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError().longValue());
        Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError().longValue());
    }

    @Test
    public void retrieveAll() throws ModuleException {
        String storage = "STAF";
        createFileReference(storage, 2 * 1024L);
        createFileReference(storage, 2 * 1024L);
        String storage2 = "HPSS";
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        createFileReference(storage2, 4 * 1024L);
        storageLocationService.monitorStorageLocations(false);
        Set<StorageLocationDTO> locs = storageLocationService.getAllLocations();
        Assert.assertNotNull("Locations should be retrieved", locs);
        Assert.assertEquals("There should be 6 locations", 6, locs.size());
        Assert.assertEquals("Location one is missing", 1L,
                            locs.stream().filter(l -> l.getName().equals(storage)).count());
        locs.stream().filter(l -> l.getName().equals(storage)).forEach(loc -> {
            Assert.assertNotNull("Configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline", loc.getConfiguration().getStorageType(),
                                StorageType.OFFLINE);
            Assert.assertEquals("There should be 2 files referenced", 2L, loc.getNbFilesStored().longValue());
            Assert.assertEquals("The total size should be 4ko", 4L, loc.getTotalStoredFilesSizeKo().longValue());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError().longValue());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError().longValue());
        });
        Assert.assertEquals("Location two is missing", 1L,
                            locs.stream().filter(l -> l.getName().equals(storage2)).count());
        locs.stream().filter(l -> l.getName().equals(storage2)).forEach(loc -> {
            Assert.assertNotNull("Configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline", loc.getConfiguration().getStorageType(),
                                StorageType.OFFLINE);
            Assert.assertEquals("There should be 5 files referenced", 5L, loc.getNbFilesStored().longValue());
            Assert.assertEquals("The total size should be 20ko", 20L, loc.getTotalStoredFilesSizeKo().longValue());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError().longValue());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError().longValue());
        });
        Assert.assertEquals("Location three is missing", 1L,
                            locs.stream().filter(l -> l.getName().equals(ONLINE_CONF_LABEL)).count());
        locs.stream().filter(l -> l.getName().equals(ONLINE_CONF_LABEL)).forEach(loc -> {
            Assert.assertNotNull("A configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline", loc.getConfiguration().getStorageType(),
                                StorageType.ONLINE);
            Assert.assertEquals("There should be 0 files referenced", 0L, loc.getNbFilesStored().longValue());
            Assert.assertEquals("The total size should be 20ko", 0L, loc.getTotalStoredFilesSizeKo().longValue());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError().longValue());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError().longValue());
        });
        Assert.assertEquals("Location four is missing", 1L,
                            locs.stream().filter(l -> l.getName().equals(NEARLINE_CONF_LABEL)).count());
        locs.stream().filter(l -> l.getName().equals(NEARLINE_CONF_LABEL)).forEach(loc -> {
            Assert.assertNotNull("A configuration should be set for the location", loc.getConfiguration());
            Assert.assertEquals("Location should be offline", loc.getConfiguration().getStorageType(),
                                StorageType.NEARLINE);
            Assert.assertEquals("There should be 0 files referenced", 0L, loc.getNbFilesStored().longValue());
            Assert.assertEquals("The total size should be 20ko", 0L, loc.getTotalStoredFilesSizeKo().longValue());
            Assert.assertEquals("There should be no storage error", 0L, loc.getNbStorageError().longValue());
            Assert.assertEquals("There should be no deletion error", 0L, loc.getNbDeletionError().longValue());
        });
    }

    @Test
    public void delete_with_files() throws ModuleException {
        String storage = "STAF";
        createFileReference(storage, 2L);
        createFileReference(storage, 2L);
        storageLocationService.monitorStorageLocations(false);
        Assert.assertNotNull("Location should exists", storageLocationService.getById(storage));
        storageLocationService.delete(storage);
        try {
            Assert.assertNull("Location should exists", storageLocationService.getById(storage));
            Assert.fail("Location should not exists anymore");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
        // As files as referenced after monitoring location should be recreated
        storageLocationService.monitorStorageLocations(false);
        Assert.assertNotNull("Location should exists", storageLocationService.getById(storage));
    }

    @Test
    public void delete_without_files() throws ModuleException {
        storageLocationService.monitorStorageLocations(false);
        Assert.assertNotNull("Location should exists", storageLocationService.getById(ONLINE_CONF_LABEL));
        storageLocationService.delete(ONLINE_CONF_LABEL);
        try {
            Assert.assertNull("Location should exists", storageLocationService.getById(ONLINE_CONF_LABEL));
            Assert.fail("Location should not exists anymore");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
        // As no files as referenced after monitoring location should not be recreated
        storageLocationService.monitorStorageLocations(false);
        try {
            Assert.assertNull("Location should exists", storageLocationService.getById(ONLINE_CONF_LABEL));
            Assert.fail("Location should not exists anymore");
        } catch (EntityNotFoundException e) {
            // Nothing to do
        }
    }
}
