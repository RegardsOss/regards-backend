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


package fr.cnes.regards.modules.ingest.service.job;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.dump.service.settings.DumpSettingsService;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.modules.workspace.service.IWorkspaceService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequest;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.IngestRequestSchedulerService;
import fr.cnes.regards.modules.ingest.service.dump.AIPSaveMetadataService;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Test for {@link AIPSaveMetadataJob}
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_savemetadata_job_it",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noscheduler" })
public class AIPSaveMetadataJobIT extends IngestMultitenantServiceIT {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private Path dumpLocation;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    private IJobService jobService;

    @Autowired
    private IAIPSaveMetadataRequestRepository metadataRequestRepository;

    @Autowired
    private DumpSettingsService dumpSettingsService;

    @Autowired
    private AIPSaveMetadataService saveMetadataService;

    @Autowired
    private IWorkspaceService workspaceService;

    @Autowired
    private StorageClientMock storageClient;

    @Autowired
    private IngestRequestSchedulerService ingestRequestSchedulerService;

    @Override
    public void doInit() throws EntityException {
        // clear before test
        dumpSettingsService.resetSettings();

        // dump location is in microservice workspace by default
        try {
            this.dumpLocation = workspaceService.getMicroserviceWorkspace();
        } catch (IOException e) {
            LOGGER.error("[AIPSaveMetadataJobIT] Not able to get microservice workspace");
        }
    }

    @Test
    @Purpose("Check if the dump of AIPs is successfully created")
    public void checkDumpSuccess() throws ExecutionException, InterruptedException, EntityException {
        // add aip to db
        int nbSIP = 6;
        storageClient.setBehavior(true, true);
        initRandomData(nbSIP, ingestRequestSchedulerService);

        // dump all aips created
        JobInfo jobInfo = runSaveMetadataJob();
        Assert.assertNotEquals(null, jobInfo);
        UUID jobInfoId = jobInfo.getId();
        ingestServiceTest.waitDuring(1000L);

        // CHECK RESULTS
        // Check job info is successful
        Optional<JobInfo> errorJobInfoOpt = jobInfoRepository.findById(jobInfoId);
        Assert.assertTrue(errorJobInfoOpt.isPresent());
        Assert.assertEquals(JobStatus.SUCCEEDED, errorJobInfoOpt.get().getStatus().getStatus());

        // Check all requests were deleted
        List<AIPSaveMetadataRequest> errorRequests = metadataRequestRepository.findAll();
        Assert.assertEquals(0, errorRequests.size());

        // Check folder target/workspace/<microservice>/ exists and contains 1 dump
        Assert.assertTrue("The dump location does not exist or does not contain one zip",
                          Files.exists(this.dumpLocation) && this.dumpLocation.toFile().listFiles().length == 1);
    }

    private JobInfo runSaveMetadataJob() throws ExecutionException, InterruptedException, EntityException {
        // Run Job and wait for end
        JobInfo saveMetadataJobInfo = saveMetadataService.scheduleJobs();
        if (saveMetadataJobInfo != null) {
            String tenant = runtimeTenantResolver.getTenant();
            jobService.runJob(saveMetadataJobInfo, tenant).get();
        }
        return saveMetadataJobInfo;

    }

    public void updateAIPProviderIdVersion() {
        List<AIPEntity> aips = aipRepository.findAll();
        for (AIPEntity aip : aips) {
            aip.setProviderId("1");
            aip.setVersion(1);
        }
        aipRepository.saveAll(aips);
    }

    @Override
    protected void doAfter() throws IOException, EntityException {
        dumpSettingsService.resetSettings();
        //clear dump location
        FileUtils.deleteDirectory(this.dumpLocation.toFile());
    }
}