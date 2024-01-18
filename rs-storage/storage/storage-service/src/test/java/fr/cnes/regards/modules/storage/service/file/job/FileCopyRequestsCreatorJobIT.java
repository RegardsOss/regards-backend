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
package fr.cnes.regards.modules.storage.service.file.job;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.fileaccess.amqp.input.FilesCopyEvent;
import fr.cnes.regards.modules.storage.service.AbstractStorageIT;
import fr.cnes.regards.modules.storage.service.StorageJobsPriority;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Tests for creation of copy requests
 *
 * @author Sébastien Binda
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_copy_job_test" },
                    locations = { "classpath:application-test.properties" })
public class FileCopyRequestsCreatorJobIT extends AbstractStorageIT {

    @Autowired
    private JobInfoService jobInfoService;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    @Test
    public void calculateCopyPath() throws MalformedURLException, ModuleException {
        Optional<Path> filePath = FileCopyRequestsCreatorJob.getDestinationFilePath(
            "file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
            Optional.ofNullable(Paths.get("/")),
            "/regards-input/storages/local/e1",
            "copied");
        Assert.assertTrue("Destination copy path should be created as the file is in the path to copy",
                          filePath.isPresent());
        Assert.assertEquals("Invalid copy destination path", "copied/f3/42/a1", filePath.get().toString());

        filePath = FileCopyRequestsCreatorJob.getDestinationFilePath(
            "file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
            Optional.ofNullable(Paths.get("/regards-input/storages/local")),
            "",
            "copied");
        Assert.assertTrue("Destination copy path should be created as the file is in the path to copy",
                          filePath.isPresent());
        Assert.assertEquals("Invalid copy destination path", "copied/e1/f3/42/a1", filePath.get().toString());

        filePath = FileCopyRequestsCreatorJob.getDestinationFilePath("file:/somewhere/referenced/files/test.xml",
                                                                     Optional.ofNullable(Paths.get(
                                                                         "/regards-input/storages/local")),
                                                                     "/",
                                                                     "copied");
        Assert.assertTrue("Destination copy path should be created as the file is in the path to copy",
                          filePath.isPresent());
        Assert.assertEquals("Invalid copy destination path",
                            "copied/somewhere/referenced/files",
                            filePath.get().toString());

        filePath = FileCopyRequestsCreatorJob.getDestinationFilePath("file:/somewhere/referenced/files/test.xml",
                                                                     Optional.ofNullable(Paths.get(
                                                                         "/regards-input/storages/local")),
                                                                     "",
                                                                     "copied");
        Assert.assertFalse(
            "Destination copy path should not be created as the file is not in path to copy. Path to copy is relative from root storage location",
            filePath.isPresent());

        filePath = FileCopyRequestsCreatorJob.getDestinationFilePath("file:/somewhere/referenced/files/test.xml",
                                                                     Optional.ofNullable(Paths.get(
                                                                         "/regards-input/storages/local")),
                                                                     "/somewhere",
                                                                     "copied");
        Assert.assertTrue("Destination copy path should be created as the file is in the path to copy",
                          filePath.isPresent());
        Assert.assertEquals("Invalid copy destination path", "copied/referenced/files", filePath.get().toString());

        filePath = FileCopyRequestsCreatorJob.getDestinationFilePath(
            "file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
            Optional.ofNullable(Paths.get("/")),
            "/regards-input/storages/local/e2",
            "copied");
        Assert.assertFalse("Destination copy path should be not created as the file is not in the path to copy",
                           filePath.isPresent());
    }

    @Test
    public void runJobWithParameters() throws InterruptedException, ExecutionException {

        // Store some files in online conf
        generateRandomStoredOnlineFileReference("file1.txt", Optional.of("files"));
        generateRandomStoredOnlineFileReference("file2.txt", Optional.of("files"));
        generateRandomStoredOnlineFileReference("file3.txt", Optional.of("files"));
        generateRandomStoredOnlineFileReference("data1.txt", Optional.of("datas"));
        generateRandomStoredOnlineFileReference("data2.txt", Optional.of("datas"));
        generateRandomStoredOnlineFileReference("data3.txt", Optional.of("datas"));
        Mockito.reset(publisher);
        // Schedule job
        String copyFrom = ONLINE_CONF_LABEL;
        String copyTo = NEARLINE_CONF_LABEL;
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_SOURCE_ID_PARMETER_NAME,
                                           copyFrom));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_DESTINATION_ID_PARMETER_NAME,
                                           copyTo));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SOURCE_PATH_PARMETER_NAME, "files"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.DESTINATION_PATH_PARMETER_NAME, "from_online"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_OWNER_PARMETER_NAME, "source1"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_PARMETER_NAME, "session1"));
        JobInfo jobInfo = new JobInfo(false,
                                      StorageJobsPriority.FILE_COPY_JOB,
                                      jobParameters,
                                      null,
                                      FileCopyRequestsCreatorJob.class.getName());
        jobInfoService.createAsPending(jobInfo);
        jobService.runJob(jobInfo, getDefaultTenant()).get();

        // Check event is well publish for copying the files
        ArgumentCaptor<FilesCopyEvent> argumentCaptor = ArgumentCaptor.forClass(FilesCopyEvent.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FilesCopyEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FilesCopyEvent copyItem = null;
        for (Object item : argumentCaptor.getAllValues()) {
            if (item instanceof FilesCopyEvent) {
                copyItem = (FilesCopyEvent) item;
                break;
            }
        }
        Assert.assertNotNull(copyItem);
        // 3 of the 6 files must be copied (only files in target/files)
        Assert.assertEquals(3, copyItem.getFiles().size());
    }

    @Test
    public void runJob() throws InterruptedException, ExecutionException {

        // Store some files in online conf
        generateRandomStoredOnlineFileReference("file1.txt", Optional.of("files"));
        generateRandomStoredOnlineFileReference("file2.txt", Optional.of("files"));
        generateRandomStoredOnlineFileReference("file3.txt", Optional.of("files"));
        generateRandomStoredOnlineFileReference("data1.txt", Optional.of("datas"));
        generateRandomStoredOnlineFileReference("data2.txt", Optional.of("datas"));
        generateRandomStoredOnlineFileReference("data3.txt", Optional.of("datas"));
        Mockito.reset(publisher);
        // Schedule job
        String copyFrom = ONLINE_CONF_LABEL;
        String copyTo = NEARLINE_CONF_LABEL;
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_SOURCE_ID_PARMETER_NAME,
                                           copyFrom));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_DESTINATION_ID_PARMETER_NAME,
                                           copyTo));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SOURCE_PATH_PARMETER_NAME, ""));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.DESTINATION_PATH_PARMETER_NAME, ""));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_OWNER_PARMETER_NAME, "source1"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_PARMETER_NAME, "session1"));
        JobInfo jobInfo = new JobInfo(false,
                                      StorageJobsPriority.FILE_COPY_JOB,
                                      jobParameters,
                                      null,
                                      FileCopyRequestsCreatorJob.class.getName());
        jobInfoService.createAsPending(jobInfo);
        jobService.runJob(jobInfo, getDefaultTenant()).get();

        // Check event is well publish for copying the files
        ArgumentCaptor<FilesCopyEvent> argumentCaptor = ArgumentCaptor.forClass(FilesCopyEvent.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(FilesCopyEvent.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        FilesCopyEvent copyItem = null;
        for (Object item : argumentCaptor.getAllValues()) {
            if (item instanceof FilesCopyEvent) {
                copyItem = (FilesCopyEvent) item;
                break;
            }
        }
        Assert.assertNotNull(copyItem);
        // All 6 files must be copied
        Assert.assertEquals(6, copyItem.getFiles().size());
    }

}
