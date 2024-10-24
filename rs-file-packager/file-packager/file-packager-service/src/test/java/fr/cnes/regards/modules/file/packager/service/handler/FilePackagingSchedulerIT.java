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
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.dao.PackageReferenceRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.filecatalog.amqp.input.FileArchiveResponseEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Test for {@link fr.cnes.regards.modules.file.packager.service.scheduler.FilePackagingScheduler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_packager_scheduler_test",
                                   "regards.file.packager.archive.max.size.in.ko=1",
                                   "regards.file.packager.archive.max.age.in.hours=12" })
public class FilePackagingSchedulerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private FilePackagerService filePackagerService;

    @Autowired
    private FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    private PackageReferenceRepository packageReferenceRepository;

    @Before
    public void setUp() {
        fileInBuildingPackageRepository.deleteAll();
        packageReferenceRepository.deleteAll();
    }

    @Test
    public void test_file_package_association_new_package() {
        //Given
        Pageable page = PageRequest.of(0, 10);
        String storage = "storage";
        String parentUrl = "https://datalake:9000/buckets/neobucket/files/family/";
        String subdirectory = "/family/";
        String parentPath = "fileaccess/cache/family/";
        long fileSize = 100L;

        long storageRequestId = 1L;
        String fileName = "bob.png";
        FileInBuildingPackage file = fileInBuildingPackageRepository.save(new FileInBuildingPackage(storageRequestId,
                                                                                                    storage,
                                                                                                    RandomChecksumUtils.generateRandomChecksum(),
                                                                                                    fileName,
                                                                                                    subdirectory,
                                                                                                    parentUrl,
                                                                                                    parentPath,
                                                                                                    fileSize));

        // No package yet
        Assertions.assertEquals(0, packageReferenceRepository.count());

        // When
        filePackagerService.associateFilesToPackage(page);

        // Then
        // Package
        Assertions.assertEquals(1, packageReferenceRepository.count(), "A new package should have been created");
        Optional<PackageReference> oPackage = packageReferenceRepository.findOneByStorageAndStorageSubdirectoryAndStatus(
            storage,
            subdirectory,
            PackageReferenceStatus.BUILDING);
        Assertions.assertTrue(oPackage.isPresent(),
                              "The created package should be for the storage and path of the saved file");
        PackageReference buildingPackage = oPackage.get();
        Assertions.assertEquals(fileSize,
                                buildingPackage.getSize(),
                                "The package size should be the same as the only file it contains");

        // File
        Optional<FileInBuildingPackage> oFile = fileInBuildingPackageRepository.findById(file.getId());
        Assertions.assertTrue(oFile.isPresent(), "The file should still exist");
        file = oFile.get();
        Assertions.assertEquals(FileInBuildingPackageStatus.BUILDING,
                                file.getStatus(),
                                "The file should now be in Building status");
        Assertions.assertEquals(buildingPackage.getId(),
                                file.getPackageReference().getId(),
                                "The file should reference the package");

        // Response Event
        ArgumentCaptor<List<FileArchiveResponseEvent>> publishedEventsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(publishedEventsCaptor.capture());
        List<FileArchiveResponseEvent> events = publishedEventsCaptor.getValue();

        Assertions.assertEquals(1, events.size(), "The should be only one response");
        FileArchiveResponseEvent response = events.get(0);
        Assertions.assertEquals(storageRequestId,
                                response.getRequestId(),
                                "The request id should be the same as the storage request id");
        Assertions.assertTrue(response.getFileUrl().startsWith(parentUrl));
        Assertions.assertTrue(response.getFileUrl().endsWith(".zip?fileName=" + fileName));
    }

    @Test
    public void test_file_package_association_existing_package_not_full() {
        //Given
        Pageable page = PageRequest.of(0, 10);
        String storage = "storage";
        String parentUrl = "https://datalake:9000/buckets/neobucket/files/family/";
        String subdirectory = "/family/";
        String parentPath = "fileaccess/cache/family/";
        long fileSize = 100L;

        long storageRequestId = 1L;
        String fileName = "bob.png";
        FileInBuildingPackage file = fileInBuildingPackageRepository.save(new FileInBuildingPackage(storageRequestId,
                                                                                                    storage,
                                                                                                    RandomChecksumUtils.generateRandomChecksum(),
                                                                                                    fileName,
                                                                                                    subdirectory,
                                                                                                    parentUrl,
                                                                                                    parentPath,
                                                                                                    fileSize));

        PackageReference existingPackage = new PackageReference(storage, subdirectory);
        long packageSize = 800L;
        existingPackage.addFileSize(packageSize);
        existingPackage = packageReferenceRepository.save(existingPackage);

        // When
        filePackagerService.associateFilesToPackage(page);

        // Then
        Assertions.assertEquals(1, packageReferenceRepository.count(), "There should be only one package");
        Optional<PackageReference> oPackage = packageReferenceRepository.findById(existingPackage.getId());
        Assertions.assertTrue(oPackage.isPresent(), "The package should still exist");
        PackageReference foundPackage = oPackage.get();

        Optional<FileInBuildingPackage> oFile = fileInBuildingPackageRepository.findById(file.getId());
        Assertions.assertTrue(oFile.isPresent(), "The file should still exist");
        FileInBuildingPackage foundFile = oFile.get();

        Assertions.assertEquals(fileSize + packageSize, foundPackage.getSize(), "The package size is not correct");
        Assertions.assertEquals(PackageReferenceStatus.BUILDING,
                                foundPackage.getStatus(),
                                "The package status should still be BUILDING");
        Assertions.assertEquals(foundPackage.getId(),
                                foundFile.getPackageReference().getId(),
                                "The file should reference the package");
    }

    @Test
    public void test_2_files_package_association_existing_package_full() {
        //Given
        Pageable page = PageRequest.of(0, 10);
        String storage = "storage";
        String parentUrl = "https://datalake:9000/buckets/neobucket/files/family/";
        String subdirectory = "/family/";
        String parentPath = "fileaccess/cache/family/";
        long fileSize = 100L;

        long storageRequestId1 = 1L;
        String fileName1 = "bob.png";
        fileInBuildingPackageRepository.save(new FileInBuildingPackage(storageRequestId1,
                                                                       storage,
                                                                       RandomChecksumUtils.generateRandomChecksum(),
                                                                       fileName1,
                                                                       subdirectory,
                                                                       parentUrl,
                                                                       parentPath,
                                                                       fileSize));

        long storageRequestId2 = 2L;
        String fileName2 = "alice.png";
        fileInBuildingPackageRepository.save(new FileInBuildingPackage(storageRequestId2,
                                                                       storage,
                                                                       RandomChecksumUtils.generateRandomChecksum(),
                                                                       fileName2,
                                                                       subdirectory,
                                                                       parentUrl,
                                                                       parentPath,
                                                                       fileSize));

        PackageReference existingPackage = new PackageReference(storage, subdirectory);
        long packageSize = 950;
        existingPackage.addFileSize(packageSize);
        existingPackage = packageReferenceRepository.save(existingPackage);

        // When
        filePackagerService.associateFilesToPackage(page);

        // Then
        Assertions.assertEquals(2, packageReferenceRepository.count(), "There should now be two packages");
        // Existing Package
        Optional<PackageReference> oPackage = packageReferenceRepository.findById(existingPackage.getId());
        Assertions.assertTrue(oPackage.isPresent(), "The package should still exist");
        PackageReference foundPackage = oPackage.get();

        Assertions.assertEquals(fileSize + packageSize, foundPackage.getSize(), "The package size is not correct");
        Assertions.assertEquals(PackageReferenceStatus.TO_STORE,
                                foundPackage.getStatus(),
                                "The package status should now be TO_STORE");

        // New Package
        oPackage = packageReferenceRepository.findOneByStorageAndStorageSubdirectoryAndStatus(storage,
                                                                                              subdirectory,
                                                                                              PackageReferenceStatus.BUILDING);
        Assertions.assertTrue(oPackage.isPresent(), "There should be an open package");
        PackageReference foundNewPackage = oPackage.get();

        Assertions.assertEquals(fileSize, foundNewPackage.getSize(), "The package size is not correct");

        // Files
        List<FileInBuildingPackage> files = fileInBuildingPackageRepository.findAll();
        Assertions.assertEquals(2, files.size(), "There should still be two files");
        Assertions.assertTrue(Objects.equals(files.get(0).getPackageReference().getId(), foundNewPackage.getId())
                              && Objects.equals(files.get(1).getPackageReference().getId(), foundPackage.getId())
                              || Objects.equals(files.get(1).getPackageReference().getId(), foundNewPackage.getId())
                                 && Objects.equals(files.get(0).getPackageReference().getId(), foundPackage.getId()),
                              "The should be one file associated to the new package and one associated to the old package");
    }

    @Test
    public void test_close_old_packages() throws NoSuchFieldException, IllegalAccessException {
        Field field = PackageReference.class.getDeclaredField("creationDate");
        field.setAccessible(true);

        // Given
        String storage = "storage";
        PackageReference oldPackage = new PackageReference(storage, "/family/abuela/");
        field.set(oldPackage, OffsetDateTime.now().minusHours(13));
        oldPackage = packageReferenceRepository.save(oldPackage);

        PackageReference youngPackage = new PackageReference(storage, "/family/nina/");
        field.set(youngPackage, OffsetDateTime.now().minusHours(11));
        youngPackage = packageReferenceRepository.save(youngPackage);

        // When
        filePackagerService.closeOldPackages();

        // Then
        oldPackage = packageReferenceRepository.findById(oldPackage.getId()).get();
        Assertions.assertEquals(PackageReferenceStatus.TO_STORE,
                                oldPackage.getStatus(),
                                "The old package should have been closed");

        oldPackage = packageReferenceRepository.findById(youngPackage.getId()).get();
        Assertions.assertEquals(PackageReferenceStatus.BUILDING,
                                oldPackage.getStatus(),
                                "The young package should not have been closed");
    }
}
