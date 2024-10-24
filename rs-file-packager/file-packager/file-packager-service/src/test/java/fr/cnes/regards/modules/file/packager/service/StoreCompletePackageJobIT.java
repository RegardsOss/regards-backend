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
package fr.cnes.regards.modules.file.packager.service;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.dao.PackageReferenceRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Test for {@link fr.cnes.regards.modules.file.packager.service.job.StoreCompletePackageJob}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=store_complete_package_job_test" })
public class StoreCompletePackageJobIT extends AbstractMultitenantServiceIT {

    @Autowired
    private FilePackagerService filePackagerService;

    @Autowired
    private FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    private PackageReferenceRepository packageReferenceRepository;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String testFilesDir;

    private static final String SUBDIRECTORY = "/family/";

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("regards.file.packager.archive.directory", () -> temporaryFolder.getRoot().toString());
    }

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException {
        fileInBuildingPackageRepository.deleteAll();
        packageReferenceRepository.deleteAll();

        testFilesDir = temporaryFolder.newFolder("cache").getAbsolutePath();
        Path familyDir = Path.of(testFilesDir, SUBDIRECTORY);
        Files.createDirectories(familyDir);

        URL resource = getClass().getClassLoader().getResource("files/file1.txt");
        Files.copy(Path.of(resource.toURI()), familyDir.resolve("file1.txt"));
        resource = getClass().getClassLoader().getResource("files/file2.txt");
        Files.copy(Path.of(resource.toURI()), familyDir.resolve("file2.txt"));

    }

    @Test
    public void test_store_complete_package() throws IOException {
        //Given
        String storage = "storage";
        String parentUrl = "https://datalake:9000/buckets/neobucket/files/family/";
        String creationDate = "20241225001122333"; //will be used instead of actual package creation date

        PackageReference packageReference = new PackageReference(storage, SUBDIRECTORY);
        packageReference.setStatus(PackageReferenceStatus.STORE_IN_PROGRESS);
        packageReference = packageReferenceRepository.save(packageReference);

        String fileName1 = "file1.txt";
        FileInBuildingPackage file1 = new FileInBuildingPackage(1L,
                                                                storage,
                                                                RandomChecksumUtils.generateRandomChecksum(),
                                                                fileName1,
                                                                SUBDIRECTORY,
                                                                parentUrl,
                                                                Path.of(testFilesDir, SUBDIRECTORY, fileName1)
                                                                    .toString(),
                                                                100L);
        file1.setPackageReference(packageReference);

        String fileName2 = "file2.txt";
        FileInBuildingPackage file2 = fileInBuildingPackageRepository.save(new FileInBuildingPackage(2L,
                                                                                                     storage,
                                                                                                     RandomChecksumUtils.generateRandomChecksum(),
                                                                                                     fileName2,
                                                                                                     SUBDIRECTORY,
                                                                                                     parentUrl,
                                                                                                     Path.of(
                                                                                                             testFilesDir,
                                                                                                             SUBDIRECTORY,
                                                                                                             fileName2)
                                                                                                         .toString(),
                                                                                                     100L));
        file2.setPackageReference(packageReference);
        fileInBuildingPackageRepository.saveAll(List.of(file1, file2));

        //When
        filePackagerService.storeCompletePackage(packageReference.getId(), SUBDIRECTORY, creationDate, storage);

        //Then
        Optional<PackageReference> oPackage = packageReferenceRepository.findById(packageReference.getId());
        Assertions.assertTrue(oPackage.isPresent(), "The package should still be there");
        Assertions.assertNotNull(oPackage.get().getChecksum(), "The archive checksum should have been set");

        ArgumentCaptor<FileStorageRequestReadyToProcessEvent> publishedEventsCaptor = ArgumentCaptor.forClass(
            FileStorageRequestReadyToProcessEvent.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(publishedEventsCaptor.capture());
        FileStorageRequestReadyToProcessEvent storageRequest = publishedEventsCaptor.getValue();

        Assertions.assertEquals(packageReference.getId(),
                                storageRequest.getRequestId(),
                                "The requestId should be equal to the package id");
        Assertions.assertEquals(oPackage.get().getChecksum(),
                                storageRequest.getChecksum(),
                                "The checksums should be the same");
        Assertions.assertEquals(Path.of(temporaryFolder.getRoot().toString(), SUBDIRECTORY, creationDate + ".zip")
                                    .toString(),
                                storageRequest.getOriginUrl(),
                                "The request archive originUrl is not the right one");

    }

}
