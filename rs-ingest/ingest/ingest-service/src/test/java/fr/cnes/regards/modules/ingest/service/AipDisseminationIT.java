/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service;

import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.integration.test.job.JobTestUtils;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.dissemination.AIPDisseminationRequestDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.job.AipDisseminationCreatorJob;
import fr.cnes.regards.modules.ingest.service.job.AipDisseminationJob;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dissemination_it",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aips.dissemination.bulk=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "noscheduler", "nojobs" }, inheritProfiles = false)
@ContextConfiguration(classes = { JobTestUtils.class })
public class AipDisseminationIT extends IngestMultitenantServiceIT {

    @Autowired
    protected AipDisseminationService aipDisseminationService;

    @Autowired
    protected IAipDisseminationRequestRepository aipDisseminationRequestRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAipDisseminationCreatorRepository disseminationCreatorRepository;

    @Autowired
    private IAipDisseminationRequestRepository disseminationRequestRepository;

    @Autowired
    private IJobService jobService;

    @Autowired
    private JobTestUtils jobTestUtils;

    private static final String SESSION_NAME = "dissemination-session";

    @Before
    public void init() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Clean everything
        ingestServiceTest.init();
    }

    protected void doInit() throws Exception {
        aipRepository.deleteAllInBatch();
        sipRepository.deleteAllInBatch();
        disseminationCreatorRepository.deleteAllInBatch();
        disseminationRequestRepository.deleteAllInBatch();
    }

    @Test
    public void testDisseminationJobWellCreated() throws ExecutionException, InterruptedException {
        // GIVEN 50 AIP and a dissemination creator
        createAIPs(50);
        PageRequest pageable = PageRequest.of(0, 100);

        AIPDisseminationRequestDto disseminationDto = new AIPDisseminationRequestDto(new SearchAIPsParameters().withSession(
            SESSION_NAME), List.of("recipient1", "recipient2"));
        // WHEN Schedule the dissemination creator
        scheduleDisseminationCreatorJob(disseminationDto);
        // THEN 50 Dissemination request (one by AIP) are created
        Assertions.assertEquals(50, aipDisseminationService.getAllRequests(pageable).getTotalElements());

        // WHEN Schedule Dissemination request
        Optional<JobInfo> jobInfo = aipDisseminationService.scheduleDisseminationJobs();
        // Then job is well created, and our 50 requests are in parameter
        Assertions.assertTrue(jobInfo.isPresent());
        Assertions.assertEquals(1, jobInfo.get().getParameters().size());
        JobParameter param = (JobParameter) jobInfo.get().getParameters().toArray()[0];
        Assertions.assertEquals(AipDisseminationJob.AIP_DISSEMINATION_REQUEST_IDS, param.getName());
        Assertions.assertEquals(50, ((List<?>) param.getValue()).size());

        // WHEN Schedule another time dissemination request
        Optional<JobInfo> jobInfo2 = aipDisseminationService.scheduleDisseminationJobs();
        // Then no job is created because all dissemination request have job associated
        Assertions.assertTrue(jobInfo2.isEmpty());
    }

    @Test
    public void testDisseminationPagination() throws ExecutionException, InterruptedException {
        // GIVEN 201 AIP and a dissemination creator
        createAIPs(201);
        PageRequest pageable = PageRequest.of(0, 1000);

        AIPDisseminationRequestDto disseminationDto = new AIPDisseminationRequestDto(new SearchAIPsParameters().withSession(
            SESSION_NAME), List.of("recipient1", "recipient2"));
        // WHEN Schedule the dissemination creator
        scheduleDisseminationCreatorJob(disseminationDto);
        // THEN 201 Dissemination request (one by AIP) are created
        Assertions.assertEquals(201, aipDisseminationService.getAllRequests(pageable).getTotalElements());

        // WHEN Schedule 3 times a Dissemination request
        IntStream.range(0, 3).forEach(i -> {
            Optional<JobInfo> jobInfo = aipDisseminationService.scheduleDisseminationJobs();
            // THEN
            // a new job info is created each 3 times because pagination (conf regards.ingest.aips.dissemination.bulk)
            // is set to 100 (and we have 201 disseminationRequest)
            Assertions.assertTrue(jobInfo.isPresent());
        });

        // WHEN Schedule another time a Dissemination request
        Optional<JobInfo> jobInfo2 = aipDisseminationService.scheduleDisseminationJobs();
        // THEN no job is created because all dissemination request have job associated
        Assertions.assertTrue(jobInfo2.isEmpty());
    }

    @Test
    public void testDisseminationJobSendToNotifier() throws ExecutionException, InterruptedException {
        // GIVEN 201 AipDisseminationRequest, inside 3 AipDisseminationJob.
        testDisseminationPagination();
        // WHEN I schedule these 3 job
        List<JobInfo> jobInfosFound = jobTestUtils.retrieveFullJobInfos(AipDisseminationJob.class, JobStatus.QUEUED);
        for (JobInfo jobInfo : jobInfosFound) {
            jobService.runJob(jobInfo, getDefaultTenant()).get();
        }
        // THEN
        Page<AipDisseminationRequest> allRequests = aipDisseminationService.getAllRequests(PageRequest.of(0, 1000));
        Assertions.assertEquals(201, allRequests.getTotalElements());
        // All dissemination is in state Waiting notifier dissemination response
        Assertions.assertTrue(allRequests.stream()
                                         .allMatch(req -> InternalRequestState.WAITING_NOTIFIER_DISSEMINATION_RESPONSE.equals(
                                             req.getState())),
                              "Dissemination requests are supposed to be in "
                              + "WAITING_NOTIFIER state after scheduling");
        // And 201 AMQP notification are send to notifier
        Mockito.verify(publisher, Mockito.times(201)).publish(Mockito.any(NotificationRequestEvent.class));
    }

    private void scheduleDisseminationCreatorJob(AIPDisseminationRequestDto disseminationDto)
        throws ExecutionException, InterruptedException {
        aipDisseminationService.registerDisseminationCreator(disseminationDto);
        List<JobInfo> jobInfosFound = jobTestUtils.retrieveFullJobInfos(AipDisseminationCreatorJob.class,
                                                                        JobStatus.QUEUED);
        Assertions.assertEquals(1, jobInfosFound.size());
        jobService.runJob(jobInfosFound.get(0), getDefaultTenant()).get();
    }

    private void createAIPs(int numberOfAips) {
        IntStream.range(0, numberOfAips)
                 .forEach(i -> createAIP("providerId" + i, Set.of(), "sessionOwner", SESSION_NAME, "storage"));
    }

    private void createAIP(String providerId,
                           Set<String> categories,
                           String sessionOwner,
                           String session,
                           String storage) {
        IngestMetadata ingestMetadata = IngestMetadata.build(sessionOwner,
                                                             session,
                                                             OffsetDateTime.now(),
                                                             IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                             categories,
                                                             VersioningMode.IGNORE,
                                                             "coucou",
                                                             StorageMetadata.build(storage));

        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "main", "test", "resources", "data", "cdpp_collection.json"),
                           "MD5",
                           "azertyuiopqsdfmlmld" + providerId);
        sip.withSyntax(MediaType.APPLICATION_JSON);
        sip.registerContentInformation();

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        SIPEntity sipEntity = SIPEntity.build(getDefaultTenant(), ingestMetadata, sip, 1, SIPState.INGESTED);
        sipEntity.setChecksum("azertyuiopqsdfmlmld" + providerId);
        sipEntity.setLastUpdate(OffsetDateTime.now().minusSeconds(1));
        sipRepository.save(sipEntity);

        AIP aip = AIP.build(sip,
                            OaisUniformResourceName.fromString(
                                "URN:AIP:DATA:CDPP:4ece80cd-7705-3ee5-babd-64c03ff61bcd:V1"),
                            Optional.empty(),
                            "providerId",
                            1);
        AIPEntity aipEntity = AIPEntity.build(sipEntity, AIPState.GENERATED, aip);

        aipRepository.save(aipEntity);
    }
}
