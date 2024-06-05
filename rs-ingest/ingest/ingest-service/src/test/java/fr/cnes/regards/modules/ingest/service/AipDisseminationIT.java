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

import com.google.gson.reflect.TypeToken;
import fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent;
import fr.cnes.regards.framework.integration.test.job.JobTestUtils;
import fr.cnes.regards.framework.modules.jobs.domain.IJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.service.IJobService;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAipDisseminationRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.IngestErrorType;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.dissemination.AipDisseminationRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.dissemination.AIPDisseminationRequestDto;
import fr.cnes.regards.modules.ingest.service.aip.scheduler.AIPUpdateRequestScheduler;
import fr.cnes.regards.modules.ingest.service.job.AipDisseminationCreatorJob;
import fr.cnes.regards.modules.ingest.service.job.AipDisseminationJob;
import fr.cnes.regards.modules.notifier.client.NotifierClient;
import fr.cnes.regards.modules.notifier.dto.out.NotificationState;
import fr.cnes.regards.modules.notifier.dto.out.NotifierEvent;
import fr.cnes.regards.modules.notifier.dto.out.Recipient;
import fr.cnes.regards.modules.notifier.dto.out.RecipientStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.reflect.Type;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Thomas GUILLOU
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dissemination_it",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.aips.dissemination.bulk=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "test", "nojobs", "noscheduler" }, inheritProfiles = false)
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
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IJobService jobService;

    @Autowired
    private JobTestUtils jobTestUtils;

    @Autowired
    private AIPUpdateRequestScheduler aipUpdateRequestScheduler;

    @SpyBean
    private NotifierClient notifierClient;

    @Captor
    private ArgumentCaptor<List<NotificationRequestEvent>> notifCapture;

    private static final String SESSION_NAME = "dissemination-session";

    private static final String ERROR_MSG = "error message";

    @Before
    public void init() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        // Clean everything
        ingestServiceTest.init(getDefaultTenant());
        Mockito.reset(notifierClient);
    }

    protected void doInit() throws Exception {
        aipRepository.deleteAllInBatch();
        sipRepository.deleteAllInBatch();
        abstractRequestRepository.deleteAllInBatch();
    }

    @Test
    public void testDisseminationJobWellCreated() throws ExecutionException, InterruptedException {
        // GIVEN 50 AIP and a dissemination creator
        createAIPs(50);
        PageRequest pageable = PageRequest.of(0, 100);

        AIPDisseminationRequestDto disseminationDto = new AIPDisseminationRequestDto(new SearchAIPsParameters().withSession(
            SESSION_NAME), Set.of("recipient1", "recipient2"));
        // WHEN Schedule the dissemination creator
        scheduleDisseminationCreatorJob(disseminationDto);
        // THEN 50 Dissemination request (one by AIP) are created
        Assertions.assertEquals(50, aipDisseminationService.findAll(pageable).getTotalElements());

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
            SESSION_NAME), Set.of("recipient1", "recipient2"));
        // WHEN Schedule the dissemination creator
        scheduleDisseminationCreatorJob(disseminationDto);
        // THEN 201 Dissemination request (one by AIP) are created
        Assertions.assertEquals(201, aipDisseminationService.findAll(pageable).getTotalElements());

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
        Page<AipDisseminationRequest> allRequests = aipDisseminationService.findAll(PageRequest.of(0, 1000));
        Assertions.assertEquals(201, allRequests.getTotalElements());
        // All dissemination is in state Waiting notifier response
        Assertions.assertTrue(allRequests.stream()
                                         .allMatch(req -> InternalRequestState.WAITING_NOTIFIER_RESPONSE.equals(req.getState())),
                              "Dissemination requests are supposed to be in "
                              + "WAITING_NOTIFIER state after scheduling");
        // each jobs (3) call one time notifierClient
        Mockito.verify(notifierClient, Mockito.times(3)).sendNotifications(notifCapture.capture());
        // And 201 AMQP notification are send to notifier
        Assertions.assertEquals(201, notifCapture.getAllValues().stream().flatMap(Collection::stream).count());
    }

    @Test
    public void testDisseminationWithNotifierSuccess() throws ExecutionException, InterruptedException {
        // GIVEN 201 AipDisseminationRequest, and notification sent to notifier
        testDisseminationJobSendToNotifier();
        Page<AipDisseminationRequest> allRequests = aipDisseminationService.findAll(PageRequest.of(0, 1000));

        Map<AbstractRequest, NotifierEvent> mapRequestEvents = new HashMap<>();
        for (AipDisseminationRequest request : allRequests) {
            Set<Recipient> recipients = request.getRecipients()
                                               .stream()
                                               .map(r -> new Recipient(r, RecipientStatus.SUCCESS, false, false))
                                               .collect(Collectors.toCollection(HashSet::new));
            mapRequestEvents.put(request,
                                 new NotifierEvent(null,
                                                   null,
                                                   NotificationState.SUCCESS,
                                                   recipients,
                                                   OffsetDateTime.now()));
        }

        // WHEN simulate notifier success response received

        notificationService.handleNotificationSuccess(mapRequestEvents);
        // first schedule : first 100 requests
        JobInfo updateJobInfo = aipUpdateRequestScheduler.scheduleJob();
        jobService.runJob(updateJobInfo, getDefaultTenant()).get();
        // second schedule : next 100 requests (200 total)
        updateJobInfo = aipUpdateRequestScheduler.scheduleJob();
        jobService.runJob(updateJobInfo, getDefaultTenant()).get();
        List<AIPEntity> allAips = aipRepository.findAll();
        Assertions.assertEquals(200,
                                allAips.stream().filter(aip -> aip.getDisseminationInfos() != null).count(),
                                "Aip dissemination should be updated !");
        // third schedule : last 1 request (201 total)
        updateJobInfo = aipUpdateRequestScheduler.scheduleJob();
        jobService.runJob(updateJobInfo, getDefaultTenant()).get();

        // THEN All AIP has been updated, and there is 1 dissemination info per recipient
        allAips = aipRepository.findAll();
        Assertions.assertEquals(201, allAips.stream().filter(aip -> aip.getDisseminationInfos() != null).count());
        Assertions.assertEquals(201 * 2, allAips.stream().mapToLong(aip -> aip.getDisseminationInfos().size()).sum());
        Assertions.assertTrue(allAips.stream()
                                     .allMatch(aip -> aip.getDisseminationInfos()
                                                         .get(0)
                                                         .getLabel()
                                                         .equals("recipient2")));
        Assertions.assertTrue(allAips.stream()
                                     .allMatch(aip -> aip.getDisseminationInfos()
                                                         .get(1)
                                                         .getLabel()
                                                         .equals("recipient1")));
    }

    @Test
    public void testDisseminationWithNotifierError() throws ExecutionException, InterruptedException {
        // GIVEN 201 AipDisseminationRequest, and notification sent to notifier
        testDisseminationJobSendToNotifier();
        Page<AipDisseminationRequest> allRequests = aipDisseminationService.findAll(PageRequest.of(0, 1000));
        // WHEN simulate notifier success response received
        notificationService.handleNotificationError(new HashSet<>(allRequests.getContent()));
        allRequests = aipDisseminationService.findAll(PageRequest.of(0, 1000));
        Assertions.assertEquals(201,
                                allRequests.stream()
                                           .filter(req -> req.getErrorType().equals(IngestErrorType.DISSEMINATION))
                                           .count());
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
                                                             "model",
                                                             StorageMetadata.build(storage));

        SIPDto sip = SIPDto.build(EntityType.DATA, providerId);
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

        AIPDto aip = AIPDto.build(sip,
                                  OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                          EntityType.DATA,
                                                                          "tenant",
                                                                          67),
                                  Optional.empty(),
                                  "providerId",
                                  1);
        AIPEntity aipEntity = AIPEntity.build(sipEntity, AIPState.GENERATED, aip);
        aipRepository.save(aipEntity);
    }

    @Test
    public void testDisseminationJobInError() throws ExecutionException, InterruptedException {

        // GIVEN 5 AIP and a dissemination creator
        createAIPs(5);
        PageRequest pageable = PageRequest.of(0, 100);
        AIPDisseminationRequestDto disseminationDto = new AIPDisseminationRequestDto(new SearchAIPsParameters().withSession(
            SESSION_NAME), Set.of("recipient1", "recipient2"));

        scheduleDisseminationCreatorJob(disseminationDto);

        // WHEN I schedule this job with an error message in the status
        Assertions.assertEquals(5, aipDisseminationService.findAll(pageable).getTotalElements());
        Optional<JobInfo> jobInfo = aipDisseminationService.scheduleDisseminationJobs();
        Assertions.assertTrue(jobInfo.isPresent());
        jobInfo.get().getStatus().setStackTrace(ERROR_MSG);
        aipDisseminationService.handleJobCrash(jobInfo.get());

        // THEN requests are updated with message and error status
        try {
            Type type = new TypeToken<Set<Long>>() {

            }.getType();
            Set<Long> ids = IJob.getValue(jobInfo.get().getParametersAsMap(),
                                          AipDisseminationJob.AIP_DISSEMINATION_REQUEST_IDS,
                                          type);
            List<AipDisseminationRequest> requests = aipDisseminationService.findAllById(new ArrayList<>(ids));
            Assertions.assertEquals(requests.size(), 5);
            requests.forEach(r -> {
                Assertions.assertEquals(r.getState(), InternalRequestState.ERROR);
                Assertions.assertTrue(r.getErrors().contains(ERROR_MSG));
            });
        } catch (JobParameterMissingException | JobParameterInvalidException e) {
            Assertions.fail("Error getting request ids from job");
        }
    }
}
