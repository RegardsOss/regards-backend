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
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEvent;
import fr.cnes.regards.framework.modules.jobs.domain.event.JobEventType;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.dao.PackageReferenceRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.domain.PackageReference;
import fr.cnes.regards.modules.file.packager.domain.PackageReferenceStatus;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.file.packager.service.job.DeleteLocalFilesJob;
import fr.cnes.regards.modules.file.packager.service.job.FileIdAndPath;
import fr.cnes.regards.modules.file.packager.service.job.PackagerJobPriority;
import fr.cnes.regards.modules.file.packager.service.job.StoreCompletePackageJob;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Test for {@link FileArchiveRequestEventHandler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_packager_job_event_test" })
public class JobEventHandlerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private JobEventHandler jobEventHandler;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    private PackageReferenceRepository packageReferenceRepository;

    @Autowired
    private FilePackagerService filePackagerService;

    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws NoSuchFieldException, IllegalAccessException, URISyntaxException, IOException {
        fileInBuildingPackageRepository.deleteAll();
        packageReferenceRepository.deleteAll();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("regards.file.packager.archive.directory", () -> temporaryFolder.getRoot().toString());
    }

    @Test
    public void test_DeleteLocalFilesJob_error() {
        // Given

        // Only the entity id is relevant for this test
        FileInBuildingPackage fileInBuildingPackage = new FileInBuildingPackage(7L,
                                                                                "storage",
                                                                                "checksum",
                                                                                "filename",
                                                                                "storageSubdirectory",
                                                                                "finalArchiveParentUrl",
                                                                                "fileCachePath",
                                                                                100L);
        fileInBuildingPackage.setStatus(FileInBuildingPackageStatus.DELETING);

        fileInBuildingPackage = fileInBuildingPackageRepository.save(fileInBuildingPackage);
        JobParameter parameter = new JobParameter(DeleteLocalFilesJob.FILES_ID_AND_PATH_PARAMETER,
                                                  List.of(new FileIdAndPath(fileInBuildingPackage.getId(),
                                                                            "fileCachePath")));
        JobInfo jobInfo = new JobInfo(false,
                                      PackagerJobPriority.DELETE_LOCAL_FILES_JOB,
                                      Set.of(parameter),
                                      "owner",
                                      DeleteLocalFilesJob.class.getName());
        jobInfo = jobInfoRepository.save(jobInfo);

        JobEvent jobEvent = new JobEvent(jobInfo.getId(), JobEventType.FAILED, DeleteLocalFilesJob.class.getName());

        // When
        jobEventHandler.handleBatch(List.of(jobEvent));

        // Then
        Optional<FileInBuildingPackage> oFileInBuildingPackage = fileInBuildingPackageRepository.findById(
            fileInBuildingPackage.getId());
        Assertions.assertTrue(oFileInBuildingPackage.isPresent(), "The File entity should still exist");
        Assertions.assertEquals(FileInBuildingPackageStatus.TO_LOCAL_DELETE,
                                oFileInBuildingPackage.get().getStatus(),
                                "The File entity should be back in TO_LOCAL_DELETE status");

    }

    @Test
    public void test_StoreCompletePackage_error() throws IOException {
        // Given
        String subdir = "subdir";
        PackageReference packageReference = new PackageReference("storage", subdir);
        packageReference.setStatus(PackageReferenceStatus.STORE_IN_PROGRESS);
        packageReference = packageReferenceRepository.save(packageReference);

        String creationDateAsString = packageReference.getCreationDate()
                                                      .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        Set<JobParameter> parameters = Set.of(new JobParameter(StoreCompletePackageJob.STORAGE_SUBDIRECTORY_PARAMETER,
                                                               subdir),
                                              new JobParameter(StoreCompletePackageJob.PACKAGE_ID_PARAMETER,
                                                               packageReference.getId()),
                                              new JobParameter(StoreCompletePackageJob.CREATION_DATE_PARAMETER,
                                                               creationDateAsString));

        JobInfo jobInfo = new JobInfo(false,
                                      PackagerJobPriority.STORE_COMPLETE_PACKAGE_JOB,
                                      parameters,
                                      "owner",
                                      StoreCompletePackageJob.class.getName());
        jobInfo = jobInfoRepository.save(jobInfo);

        JobEvent jobEvent = new JobEvent(jobInfo.getId(), JobEventType.FAILED, StoreCompletePackageJob.class.getName());

        // Simulate malformed archive remaining
        Path archivePath = temporaryFolder.getRoot()
                                          .toPath()
                                          .resolve(filePackagerService.getArchivePath(subdir, creationDateAsString));
        Files.createDirectories(archivePath.getParent());
        Files.createFile(archivePath);

        // When
        jobEventHandler.handleBatch(List.of(jobEvent));

        // Then
        Optional<PackageReference> optionalPackageReference = packageReferenceRepository.findById(packageReference.getId());
        Assertions.assertTrue(optionalPackageReference.isPresent(), "The Package entity should still exist");
        Assertions.assertEquals(PackageReferenceStatus.STORE_ERROR,
                                optionalPackageReference.get().getStatus(),
                                "The Package entity should now be in STORE_ERROR status");

        Assertions.assertFalse(Files.exists(archivePath),
                               "The archive that remained after the job abortion should have been deleted");
    }
}
