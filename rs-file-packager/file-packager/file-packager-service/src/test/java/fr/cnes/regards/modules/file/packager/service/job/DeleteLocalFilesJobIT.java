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
package fr.cnes.regards.modules.file.packager.service.job;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Optional;

/**
 * Test class for {@link fr.cnes.regards.modules.file.packager.service.job.DeleteLocalFilesJob}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=delete_local_files_job_test" })
public class DeleteLocalFilesJobIT extends AbstractMultitenantServiceIT {

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Autowired
    public FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    public FilePackagerService filePackagerService;

    private File lockedDir;

    @Before
    public void setUp() throws IOException {
        fileInBuildingPackageRepository.deleteAll();
        lockedDir = temporaryFolder.newFolder();
    }

    @After
    public void cleanUp() throws IOException {
        Files.setPosixFilePermissions(lockedDir.toPath(), PosixFilePermissions.fromString("rwxr-xr-x"));
    }

    @Test
    public void test_delete_one_success_one_error() throws IOException {
        // Given
        File successFile = temporaryFolder.newFile();

        File nonExistingFile = temporaryFolder.newFile();
        Files.delete(nonExistingFile.toPath());

        Path errorFile = Files.createFile(lockedDir.toPath().resolve("error.tmp"));
        Files.setPosixFilePermissions(lockedDir.toPath(), PosixFilePermissions.fromString("r--r--r--"));

        FileInBuildingPackage successEntity = new FileInBuildingPackage(0L,
                                                                        "",
                                                                        RandomChecksumUtils.generateRandomChecksum(),
                                                                        "0",
                                                                        "",
                                                                        "",
                                                                        "",
                                                                        0L);
        successEntity.setStatus(FileInBuildingPackageStatus.TO_LOCAL_DELETE);
        successEntity = fileInBuildingPackageRepository.save(successEntity);

        FileInBuildingPackage errorEntity = new FileInBuildingPackage(1L,
                                                                      "",
                                                                      RandomChecksumUtils.generateRandomChecksum(),
                                                                      "1",
                                                                      "",
                                                                      "",
                                                                      "",
                                                                      0L);
        errorEntity.setStatus(FileInBuildingPackageStatus.TO_LOCAL_DELETE);
        errorEntity = fileInBuildingPackageRepository.save(errorEntity);

        FileInBuildingPackage nonExistingFileEntity = new FileInBuildingPackage(2L,
                                                                                "",
                                                                                RandomChecksumUtils.generateRandomChecksum(),
                                                                                "2",
                                                                                "",
                                                                                "",
                                                                                "",
                                                                                0L);
        nonExistingFileEntity.setStatus(FileInBuildingPackageStatus.TO_LOCAL_DELETE);
        nonExistingFileEntity = fileInBuildingPackageRepository.save(nonExistingFileEntity);

        // When
        filePackagerService.deleteLocalFiles(List.of(new FileIdAndPath(successEntity.getId(),
                                                                       successFile.getAbsolutePath()),
                                                     new FileIdAndPath(errorEntity.getId(), errorFile.toString()),
                                                     new FileIdAndPath(nonExistingFileEntity.getId(),
                                                                       nonExistingFile.getAbsolutePath())));

        // Then
        Assertions.assertFalse(Files.exists(successFile.toPath()), "The file should have been deleted");
        Assertions.assertTrue(fileInBuildingPackageRepository.findById(successEntity.getId()).isEmpty(),
                              "The entity should have been deleted");

        Assertions.assertTrue(fileInBuildingPackageRepository.findById(nonExistingFileEntity.getId()).isEmpty(),
                              "The entity should have been deleted");

        Optional<FileInBuildingPackage> oEntity = fileInBuildingPackageRepository.findById(errorEntity.getId());
        Assertions.assertTrue(oEntity.isPresent(), "The entity should still be there as there was an error");
        Assertions.assertEquals(FileInBuildingPackageStatus.DELETION_ERROR,
                                oEntity.get().getStatus(),
                                "The entity should now be in DELETION_ERROR status");
    }
}
