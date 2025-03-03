/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.file.packager.amqp.FileArchiveCompletionEvent;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.dao.PackageReferenceRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import fr.cnes.regards.modules.file.packager.dto.FileArchiveCompletionDto;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.fileaccess.amqp.output.StorageResponseEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Optional;

/**
 * Test for {@link StorageResponseEventHandler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_packager_storage_response_test" })
public class StorageResponseEventHandlerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private PackageReferenceRepository packageReferenceRepository;

    @Autowired
    private FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    private FilePackagerService filePackagerService;

    @Autowired
    IRuntimeTenantResolver runtimeTenantResolver;

    @Value("${spring.application.name}")
    private String applicationName;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        fileInBuildingPackageRepository.deleteAll();
        packageReferenceRepository.deleteAll();
        simulateApplicationReadyEvent();
    }

    @Test
    public void test_update_package_after_completion_success() {
        // Given
        String storage = "storage1";
        String storageSubdirectory = "node";
        PackageReference savedPackage = packageReferenceRepository.save(new PackageReference(storage,
                                                                                             storageSubdirectory));
        String checksum1 = RandomChecksumUtils.generateRandomChecksum();
        FileInBuildingPackage fileToSave = new FileInBuildingPackage(4242L,
                                                                     storage,
                                                                     checksum1,
                                                                     "file1.txt",
                                                                     storageSubdirectory,
                                                                     "https://s3.com/bucket/node/12345.zip",
                                                                     "workspace/file1.txt",
                                                                     100L);
        fileToSave.setPackageReference(savedPackage);
        FileInBuildingPackage savedFile1 = fileInBuildingPackageRepository.save(fileToSave);

        String checksum2 = RandomChecksumUtils.generateRandomChecksum();
        fileToSave = new FileInBuildingPackage(4343L,
                                               storage,
                                               checksum2,
                                               "file2.txt",
                                               storageSubdirectory,
                                               "https://s3.com/bucket/node/12345.zip",
                                               "workspace/file2.txt",
                                               100L);
        fileToSave.setPackageReference(savedPackage);
        FileInBuildingPackage savedFile2 = fileInBuildingPackageRepository.save(fileToSave);

        // Only the requestId and error fields are used in package storage.
        StorageResponseEvent event = StorageResponseEvent.createSuccessResponse(applicationName
                                                                                + "."
                                                                                + savedPackage.getId(), "", "");

        // When
        filePackagerService.updatePackageAfterCompletion(List.of(event));

        // Then
        Optional<PackageReference> foundPackage = packageReferenceRepository.findById(savedPackage.getId());
        Assertions.assertTrue(foundPackage.isPresent(), "The package should still be there");
        Assertions.assertEquals(PackageReferenceStatus.STORED,
                                foundPackage.get().getStatus(),
                                "The package should now be STORED");

        Optional<FileInBuildingPackage> foundFile = fileInBuildingPackageRepository.findById(savedFile1.getId());
        Assertions.assertTrue(foundFile.isPresent(), "The file should still be there");
        Assertions.assertEquals(FileInBuildingPackageStatus.TO_LOCAL_DELETE,
                                foundFile.get().getStatus(),
                                "The file should now be TO_DELETE");

        foundFile = fileInBuildingPackageRepository.findById(savedFile2.getId());
        Assertions.assertTrue(foundFile.isPresent(), "The file should still be there");
        Assertions.assertEquals(FileInBuildingPackageStatus.TO_LOCAL_DELETE,
                                foundFile.get().getStatus(),
                                "The file should now be TO_DELETE");

        ArgumentCaptor<FileArchiveCompletionEvent> publishedEventsCaptor = ArgumentCaptor.forClass(
            FileArchiveCompletionEvent.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(publishedEventsCaptor.capture());
        List<String> completedChecksums = publishedEventsCaptor.getAllValues()
                                                               .stream()
                                                               .map(FileArchiveCompletionDto::getChecksum)
                                                               .toList();
        Assertions.assertTrue(completedChecksums.contains(checksum1), "There should be an event for the first file");
        Assertions.assertTrue(completedChecksums.contains(checksum2), "There should be an event for the second file");

    }

    @Test
    public void test_update_package_after_completion_error() {
        // Given
        String storage = "storage1";
        String storageSubdirectory = "node";
        PackageReference savedPackage = packageReferenceRepository.save(new PackageReference(storage,
                                                                                             storageSubdirectory));
        FileInBuildingPackage fileToSave = new FileInBuildingPackage(4242L,
                                                                     storage,
                                                                     RandomChecksumUtils.generateRandomChecksum(),
                                                                     "file1.txt",
                                                                     storageSubdirectory,
                                                                     "https://s3.com/bucket/node/12345.zip",
                                                                     "workspace/file1.txt",
                                                                     100L);
        fileToSave.setPackageReference(savedPackage);
        FileInBuildingPackage savedFile = fileInBuildingPackageRepository.save(fileToSave);

        // Only the requestId and error fields are used in package storage.
        String errorMessage = "Error !";
        StorageResponseEvent event = StorageResponseEvent.createErrorResponse(applicationName
                                                                              + "."
                                                                              + savedPackage.getId(),
                                                                              "",
                                                                              "",
                                                                              StorageResponseErrorEnum.WORKER_ERROR,
                                                                              errorMessage);

        // When
        filePackagerService.updatePackageAfterCompletion(List.of(event));

        // Then
        Optional<PackageReference> foundPackage = packageReferenceRepository.findById(savedPackage.getId());
        Assertions.assertTrue(foundPackage.isPresent(), "The package should still be there");
        Assertions.assertEquals(PackageReferenceStatus.STORE_ERROR,
                                foundPackage.get().getStatus(),
                                "The package should now be in STORE_ERROR");
        Assertions.assertEquals(errorMessage, foundPackage.get().getErrorCause(), "There should be an error message");

        Optional<FileInBuildingPackage> foundFile = fileInBuildingPackageRepository.findById(savedFile.getId());
        Assertions.assertTrue(foundFile.isPresent(), "The file should still be there");
        Assertions.assertNotEquals(FileInBuildingPackageStatus.TO_LOCAL_DELETE,
                                   foundFile.get().getStatus(),
                                   "The file should not be TO_DELETE");

        ArgumentCaptor<FileArchiveCompletionEvent> publishedEventsCaptor = ArgumentCaptor.forClass(
            FileArchiveCompletionEvent.class);
        Mockito.verify(publisher, Mockito.times(0)).publish(publishedEventsCaptor.capture());

    }
}
