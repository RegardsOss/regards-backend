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
package fr.cnes.regards.modules.storagelight.service;

import java.util.List;
import java.util.UUID;

import org.apache.commons.compress.utils.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storagelight.dao.IFileReferenceRepository;
import fr.cnes.regards.modules.storagelight.dao.IStorageLocationRepository;
import fr.cnes.regards.modules.storagelight.dao.IStorageMonitoringRepository;
import fr.cnes.regards.modules.storagelight.domain.database.FileLocation;
import fr.cnes.regards.modules.storagelight.domain.database.FileReferenceMetaInfo;

/**
 * @author sbinda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_tests",
        "regards.storage.cache.path=target/cache", "regards.storage.cache.minimum.time.to.live.hours=12" })
public class StorageLocationServiceTest extends AbstractMultitenantServiceTest {

    @Autowired
    private StorageLocationService storageLocationService;

    @Autowired
    private IFileReferenceRepository fileRefRepo;

    @Autowired
    private IStorageLocationRepository storageLocationRepo;

    @Autowired
    private IStorageMonitoringRepository storageMonitorRepo;

    @Autowired
    private FileReferenceService fileRefService;

    @Before
    public void init() throws ModuleException {
        fileRefRepo.deleteAll();
        storageLocationRepo.deleteAll();
        storageMonitorRepo.deleteAll();
    }

    private void createFileReference(String storage, Long fileSize) {
        List<String> owners = Lists.newArrayList();
        owners.add("someone");
        String checksum = UUID.randomUUID().toString();
        FileReferenceMetaInfo fileMetaInfo = new FileReferenceMetaInfo(checksum, "MD5", "file.test", fileSize,
                MediaType.APPLICATION_OCTET_STREAM);
        FileLocation origin = new FileLocation(storage, "anywhere://in/this/directory/" + checksum);
        fileRefService.addFileReference(owners, fileMetaInfo, origin, origin);
    }

    @Test
    public void monitorStorageLocation() {
        Long totalSize = 0L;
        String storage = "STAF";
        Assert.assertFalse("0. There not have file referenced yet", storageLocationService.search(storage).isPresent());
        storageLocationService.monitorDataStorages();
        Assert.assertFalse("1. There not have file referenced yet", storageLocationService.search(storage).isPresent());
        createFileReference(storage, 1L);
        totalSize++;
        createFileReference(storage, 1L);
        totalSize++;
        createFileReference(storage, 1L);
        totalSize++;
        createFileReference(storage, 1L);
        totalSize++;
        storageLocationService.monitorDataStorages();
        Assert.assertTrue("There should be file referenced for STAF storage",
                          storageLocationService.search(storage).isPresent());
        Assert.assertEquals("Total size on STAF storage is invalid", totalSize.longValue(),
                            storageLocationService.search(storage).get().getTotalSizeOfReferencedFiles().longValue());
        Assert.assertEquals("Total number of files on STAF storage is invalid", 4L,
                            storageLocationService.search(storage).get().getNumberOfReferencedFiles().longValue());
        createFileReference(storage, 3L);
        totalSize += 3;
        storageLocationService.monitorDataStorages();
        Assert.assertTrue("There should be file referenced for STAF storage",
                          storageLocationService.search(storage).isPresent());
        Assert.assertEquals("Total size on STAF storage is invalid", totalSize.longValue(),
                            storageLocationService.search(storage).get().getTotalSizeOfReferencedFiles().longValue());
        Assert.assertEquals("Total number of files on STAF storage invalid", 5L,
                            storageLocationService.search(storage).get().getNumberOfReferencedFiles().longValue());

    }

}
