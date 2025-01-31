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
package fr.cnes.regards.modules.file.packager.service.scheduler;

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.framework.test.integration.RandomChecksumUtils;
import fr.cnes.regards.modules.file.packager.dao.FileInBuildingPackageRepository;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackage;
import fr.cnes.regards.modules.file.packager.domain.FileInBuildingPackageStatus;
import fr.cnes.regards.modules.file.packager.service.FilePackagerService;
import fr.cnes.regards.modules.file.packager.service.job.DeleteLocalFilesJob;
import fr.cnes.regards.modules.file.packager.service.job.FileIdAndPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Test class for {@link FileDeletingScheduler}
 *
 * @author Thibaud Michaudel
 **/
@ActiveProfiles({ "nojobs", "noscheduler", "test" })
@SpringBootTest
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=file_deleting_scheduler_test",
                                   "regards.file.packager.archive.max.size.in.ko=1",
                                   "regards.file.packager.archive.max.age.in.hours=12" })
public class FileDeletingSchedulerIT extends AbstractMultitenantServiceIT {

    @Autowired
    private FileInBuildingPackageRepository fileInBuildingPackageRepository;

    @Autowired
    private FilePackagerService filePackagerService;

    @MockBean
    private JobInfoService jobInfoService;

    @Before
    public void init() {
        runtimeTenantResolver.forceTenant(this.getDefaultTenant());
        fileInBuildingPackageRepository.deleteAll();
        simulateApplicationReadyEvent();
    }

    @Test
    public void schedule_deletion_jobs_test() throws JobParameterMissingException, JobParameterInvalidException {
        // Given
        String storage = "storage1";
        String storageSubdirectory = "node";

        FileInBuildingPackage fileToSave = new FileInBuildingPackage(4242L,
                                                                     storage,
                                                                     RandomChecksumUtils.generateRandomChecksum(),
                                                                     "file1.txt",
                                                                     storageSubdirectory,
                                                                     "https://s3.com/bucket/node/12345.zip",
                                                                     "workspace/file1.txt",
                                                                     100L);
        fileToSave.setKeepInCacheUntilDate(OffsetDateTime.now().minusHours(5));
        fileToSave.setStatus(FileInBuildingPackageStatus.TO_LOCAL_DELETE);
        FileInBuildingPackage savedFile1 = fileInBuildingPackageRepository.save(fileToSave);

        fileToSave = new FileInBuildingPackage(42343L,
                                               storage,
                                               RandomChecksumUtils.generateRandomChecksum(),
                                               "file2.txt",
                                               storageSubdirectory,
                                               "https://s3.com/bucket/node/12345.zip",
                                               "workspace/file2.txt",
                                               100L);
        fileToSave.setKeepInCacheUntilDate(OffsetDateTime.now().plusHours(5));
        fileToSave.setStatus(FileInBuildingPackageStatus.TO_LOCAL_DELETE);
        FileInBuildingPackage savedFile2 = fileInBuildingPackageRepository.save(fileToSave);

        // When
        filePackagerService.scheduleDeleteLocalFilesJobs();

        // Then
        Optional<FileInBuildingPackage> foundFile = fileInBuildingPackageRepository.findById(savedFile1.getId());
        Assertions.assertTrue(foundFile.isPresent(), "The file should still be there");
        Assertions.assertEquals(FileInBuildingPackageStatus.DELETING,
                                foundFile.get().getStatus(),
                                "The file should now be DELETING");

        foundFile = fileInBuildingPackageRepository.findById(savedFile2.getId());
        Assertions.assertTrue(foundFile.isPresent(), "The file should still be there");
        Assertions.assertEquals(FileInBuildingPackageStatus.TO_LOCAL_DELETE,
                                foundFile.get().getStatus(),
                                "The file should still be TO_LOCAL_DELETE because it need to be kept in cache");

        ArgumentCaptor<JobInfo> jobInfoArgumentCaptor = ArgumentCaptor.forClass(JobInfo.class);
        Mockito.verify(jobInfoService, Mockito.times(1)).createAsQueued(jobInfoArgumentCaptor.capture());
        JobInfo jobInfo = jobInfoArgumentCaptor.getValue();
        List<FileIdAndPath> jobParam = IJob.getValue(jobInfo.getParametersAsMap(),
                                                     DeleteLocalFilesJob.FILES_ID_AND_PATH_PARAMETER,
                                                     new TypeToken<List<FileIdAndPath>>() {

                                                     }.getType());
        Assertions.assertEquals(savedFile1.getId(),
                                jobParam.get(0).fileId(),
                                "The file id in the job is not the expected one");
    }
}
