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
package fr.cnes.regards.modules.file.packager.service.handler;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileArchiveRequestEvent;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link FileArchiveRequestEventHandler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_packager_archive_requests_test" })
public class FileArchiveRequestEventHandlerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private FileArchiveRequestEventHandler fileArchiveRequestEventHandler;

    @Autowired
    private FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        fileInBuildingPackageRepository.deleteAll();
        simulateApplicationReadyEvent();
    }

    @Test
    public void test_file_in_building_package_creation() {
        String storage = "storage";
        String storeParentPath = "/packager/parent/path/";
        String storeParentUrl = "https://glacier.com/bucket/";
        String storageSubDirectory = "";
        long storageRequestId1 = 1L;
        String checksum1 = "2ab4490f51d06597dc9f7a967996080f";
        String fileName1 = "file1.txt";
        long storageRequestId2 = 2L;
        String checksum2 = "e7a1ef1c0fd3f398eb8eddb62a66c1a0";
        String fileName2 = "file2.txt";

        FileArchiveRequestEvent fileArchiveRequestEvent1 = new FileArchiveRequestEvent(storageRequestId1,
                                                                                       storage,
                                                                                       checksum1,
                                                                                       fileName1,
                                                                                       storageSubDirectory,
                                                                                       storeParentUrl,
                                                                                       storeParentPath,
                                                                                       100L);

        FileArchiveRequestEvent fileArchiveRequestEvent2 = new FileArchiveRequestEvent(storageRequestId2,
                                                                                       storage,
                                                                                       checksum2,
                                                                                       fileName2,
                                                                                       storageSubDirectory,
                                                                                       storeParentUrl,
                                                                                       storeParentPath,
                                                                                       100L);
        fileArchiveRequestEventHandler.handleBatch(List.of(fileArchiveRequestEvent1, fileArchiveRequestEvent2));

        String tenant = runtimeTenantResolver.getTenant();
        Awaitility.await().atMost(10, TimeUnit.SECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            return fileInBuildingPackageRepository.findAll().size() == 2;
        });

        List<FileInBuildingPackage> files = fileInBuildingPackageRepository.findAll();
        Optional<FileInBuildingPackage> oFile1 = files.stream()
                                                      .filter(file -> file.getFilename().equals(fileName1))
                                                      .findAny();
        if (oFile1.isEmpty()) {
            Assertions.fail("The file corresponding to the first event was not stored");
        }
        FileInBuildingPackage file1 = oFile1.get();
        Assertions.assertEquals(storage, file1.getStorage());
        Assertions.assertEquals(storageRequestId1, file1.getStorageRequestId());
        Assertions.assertEquals(checksum1, file1.getChecksum());
        Assertions.assertEquals(fileName1, file1.getFilename());
        Assertions.assertEquals(storageSubDirectory, file1.getStorageSubdirectory());
        Assertions.assertEquals(storeParentUrl, file1.getFinalArchiveParentUrl());
        Assertions.assertEquals(FileInBuildingPackageStatus.WAITING_PACKAGE, file1.getStatus());

        Optional<FileInBuildingPackage> oFile2 = files.stream()
                                                      .filter(file -> file.getFilename().equals(fileName2))
                                                      .findAny();
        if (oFile2.isEmpty()) {
            Assertions.fail("The file corresponding to the first event was not stored");
        }
        FileInBuildingPackage file2 = oFile2.get();
        Assertions.assertEquals(storageRequestId2, file2.getStorageRequestId());
        Assertions.assertEquals(storage, file2.getStorage());
        Assertions.assertEquals(checksum2, file2.getChecksum());
        Assertions.assertEquals(fileName2, file2.getFilename());
        Assertions.assertEquals(storageSubDirectory, file2.getStorageSubdirectory());
        Assertions.assertEquals(storeParentUrl, file2.getFinalArchiveParentUrl());
        Assertions.assertEquals(FileInBuildingPackageStatus.WAITING_PACKAGE, file2.getStatus());

    }
}
