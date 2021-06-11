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
package fr.cnes.regards.modules.storage.service.file.job;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.JobInfoService;
import fr.cnes.regards.modules.storage.domain.flow.CopyFlowItem;
import fr.cnes.regards.modules.storage.service.AbstractStorageTest;
import fr.cnes.regards.modules.storage.service.JobsPriority;

/**
 * Tests for creation of copy requests
 *
 * @author Sébastien Binda
 *
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=storage_copy_job_test",
        "regards.storage.cache.path=target/cache" }, locations = { "classpath:application-test.properties" })
public class FileCopyRequestsCreatorJobTest extends AbstractStorageTest {

    @Autowired
    private JobInfoService jobInfoService;

    @Before
    public void initialize() throws ModuleException {
        Mockito.clearInvocations(publisher);
        super.init();
    }

    @Test
    public void calculateCopyPath() throws MalformedURLException, ModuleException {
        Optional<Path> filePath = FileCopyRequestsCreatorJob
                .getDestinationFilePath("file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
                                        "/regards-input/storages/local/e1", "copied");
        Assert.assertTrue("Destination copy path should be created as the file is in the path to copy",
                          filePath.isPresent());
        Assert.assertEquals("Invalid copy destination path", "copied/f3/42/a1", filePath.get().toString());

        filePath = FileCopyRequestsCreatorJob
                .getDestinationFilePath("file:/regards-input/storages/local/e1/f3/42/a1/123456789132456789",
                                        "/regards-input/storages/local/e2", "copied");
        Assert.assertFalse("Destination copy path should be not created as the file is not in the path to copy",
                           filePath.isPresent());
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
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_SOURCE_ID, copyFrom));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.STORAGE_LOCATION_DESTINATION_ID, copyTo));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SOURCE_PATH, "target/storage-online/files"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.DESTINATION_PATH, "from_online"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION_OWNER, "source1"));
        jobParameters.add(new JobParameter(FileCopyRequestsCreatorJob.SESSION, "session1"));
        JobInfo jobInfo = new JobInfo(false, JobsPriority.FILE_COPY_JOB.getPriority(), jobParameters, null,
                FileCopyRequestsCreatorJob.class.getName());
        jobInfoService.createAsPending(jobInfo);
        jobService.runJob(jobInfo, getDefaultTenant()).get();

        // Check event is well publish for copying the files
        ArgumentCaptor<CopyFlowItem> argumentCaptor = ArgumentCaptor.forClass(CopyFlowItem.class);
        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(CopyFlowItem.class));
        Mockito.verify(this.publisher, Mockito.atLeastOnce()).publish(argumentCaptor.capture());
        CopyFlowItem copyItem = null;
        for (Object item : argumentCaptor.getAllValues()) {
            if (item instanceof CopyFlowItem) {
                copyItem = (CopyFlowItem) item;
                break;
            }
        }
        Assert.assertNotNull(copyItem);
        // 3 of the 6 files must be copied (only files in target/files)
        Assert.assertEquals(3, copyItem.getFiles().size());
    }

}
