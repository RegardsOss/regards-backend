/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAIPDumpMetadataRepositoryRefactor;
import fr.cnes.regards.modules.ingest.dao.IAIPSaveMetadataRequestRepositoryRefactor;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.exception.DuplicateUniqueNameException;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dump.AIPSaveMetadataRequestRefactor;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.aip.AIPMetadataServiceRefactor;
import fr.cnes.regards.modules.ingest.service.aip.AIPSaveMetadataServiceRefactor;
import static fr.cnes.regards.modules.ingest.service.job.TestData.*;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;

/**
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_savemetadata_refactor_test",
        "regards.amqp.enabled=true", "regards.dump.location=target/workspace" },
        locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noschedule" })
public class AIPSaveMetadataJobRefactorIT extends IngestMultitenantServiceTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Value("${regards.dump.location}")
    private String dumpLocation;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IAIPSaveMetadataRequestRepositoryRefactor metadataRequestRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    @Autowired
    IAIPDumpMetadataRepositoryRefactor dumpRepository;

    @Autowired
    AIPSaveMetadataServiceRefactor saveMetadataService;

    @Autowired
    AIPMetadataServiceRefactor metadataService;

    @Autowired
    private IJobService jobService;

    @Autowired
    private StorageClientMock storageClient;

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        abstractRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
    }

    @Test
    @Purpose("Check if the dump of AIPs is successfully created")
    public void checkDumpSuccess() throws ExecutionException, InterruptedException {
        // add aip to db
        int nbSIP = 6;
        storageClient.setBehavior(true, true);
        initData(nbSIP);
        // set last update date to null
        metadataService.resetLastUpdateDate();
        // dump all aips created
        UUID jobInfoId = runSaveMetadataJob();

        Thread.sleep(1000);

        // CHECK RESULTS
        // Check job info is successful
        Optional<JobInfo> errorJobInfoOpt = jobInfoRepository.findById(jobInfoId);
        Assert.assertTrue(errorJobInfoOpt.isPresent());
        Assert.assertEquals(JobStatus.SUCCEEDED, errorJobInfoOpt.get().getStatus().getStatus());

        // Check all requests were deleted
        List<AIPSaveMetadataRequestRefactor> errorRequests = metadataRequestRepository.findAll();
        Assert.assertEquals(0, errorRequests.size());


        // job has finished lets check result in DB & dump
        // first check that job has finished successfully i.e. JobInfo is in success
        // then check that there is no dump request left in DB(request in success are automatically deleted)

        //FIXME: use when old request(STORE_META) have been removed
        //ingestServiceTest.waitAllRequestsFinished(nbSIP * 5000);
       /* Assert.assertEquals(1, Paths.get(this.dumpLocation).resolve(runtimeTenantResolver.getTenant()).resolve(microservice).toFile()
                .listFiles().length);*/
    }

    @Test
    @Purpose("Check if the dump of AIPs is not created (in error)")
    public void checkDumpError() {
        // add aip to db
        int nbSIP = 3;
        storageClient.setBehavior(true, true);
        initData(nbSIP);

        // change provider id to create duplicated jsonNames
        updateAIPProviderIdVersion();

        // set last update date to null
        metadataService.resetLastUpdateDate();

        // dump all aips created
        try {
            runSaveMetadataJob();
            Assert.fail();
        } catch (ExecutionException | InterruptedException e) {
            Assert.assertTrue("DuplicateUniqueNameException was expected",
                              e.getMessage().contains("DuplicateUniqueNameException"));
        }
        // Check job info in ERROR
        Pageable pageToRequest = PageRequest.of(0, 100);
        Page<JobInfo> errorJobInfo =  jobInfoRepository.findByClassNameAndStatusStatusIn(AIPSaveMetadataJobRefactor.class.getName(), JobStatus.values(),
                                                                                         pageToRequest);
        Assert.assertEquals(1L, errorJobInfo.getTotalElements());
        //Assert.assertEquals(JobStatus.FAILED, errorJobInfo.getContent().get(0).getStatus().getStatus().toString());

        // Check request in ERROR
        List<AIPSaveMetadataRequestRefactor> errorRequests = metadataRequestRepository.findAll();
        Assert.assertEquals(1, errorRequests.size());
        Assert.assertEquals(InternalRequestState.ERROR, errorRequests.get(0).getState());
    }

    private UUID runSaveMetadataJob() throws ExecutionException, InterruptedException {
        // Run Job and wait for end
        JobInfo saveMetadataJob = saveMetadataService.scheduleJobs();
        String tenant = runtimeTenantResolver.getTenant();
        jobService.runJob(saveMetadataJob, tenant).get();
        return saveMetadataJob.getId();

    }

    public void initData(int nbSIP) {
        for (int i = 0; i < nbSIP; i++) {
            publishSIPEvent(create(UUID.randomUUID().toString(), getRandomTags()), getRandomStorage().get(0),
                            getRandomSession(), getRandomSessionOwner(), getRandomCategories());
        }
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 5000, SIPState.STORED);
        // Wait STORE_META request over
        //ingestServiceTest.waitAllRequestsFinished(nbSIP * 5000);
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
    protected void doAfter() throws IOException {
        // clean tables (dump + sip + aip + request + job)
        abstractRequestRepository.deleteAll();
        jobInfoRepository.deleteAll();
        aipRepository.deleteAll();
        sipRepository.deleteAll();
        dumpRepository.deleteAll();
        //clear dump location
        //FileUtils.deleteDirectory(Paths.get(this.dumpLocation).toFile());
    }
}