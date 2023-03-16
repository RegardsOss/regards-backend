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
package fr.cnes.regards.modules.ingest.service.request;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorPayload;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_service_schedule_it",
                                   "regards.amqp.enabled=true",
                                   "eureka.client.enabled=false",
                                   "regards.ingest.maxBulkSize=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noscheduler" })
public class RequestServiceScheduleIT extends AbstractIngestRequestIT {

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private RequestService requestService;

    @Autowired
    private IJobInfoRepository jobInfoRepository;

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    public static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

    private List<AIPEntity> aips;

    public void prepareOAISEntities() {
        SIPEntity sip4 = new SIPEntity();

        sip4.setSip(SIP.build(EntityType.DATA, "SIP_001").withDescriptiveInformation("version", "2"));
        sip4.setSipId(OaisUniformResourceName.fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID() + ":V1"));
        sip4.setProviderId("SIP_003");
        sip4.setCreationDate(OffsetDateTime.now().minusHours(6));
        sip4.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sip4.setSessionOwner("SESSION_OWNER");
        sip4.setSession("SESSION");
        sip4.setCategories(org.assertj.core.util.Sets.newLinkedHashSet("CATEGORIES"));
        sip4.setState(SIPState.INGESTED);
        sip4.setVersion(2);
        sip4.setChecksum("123456789032");

        sip4 = sipRepository.save(sip4);

        AIP aip = AIP.build(sip4.getSip(),
                            OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 1),
                            Optional.empty(),
                            "SIP_001",
                            sip4.getVersion());
        AIPEntity aipEntity = AIPEntity.build(sip4, AIPState.GENERATED, aip);

        aipEntity = aipRepository.save(aipEntity);

        AIP aip2 = AIP.build(sip4.getSip(),
                             OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 1),
                             Optional.empty(),
                             "SIP_002",
                             sip4.getVersion());
        AIPEntity aipEntity2 = AIPEntity.build(sip4, AIPState.GENERATED, aip2);

        aipEntity2 = aipRepository.save(aipEntity2);

        aips = aipRepository.findAll();

        LOGGER.info("=========================> END INIT DATA FOR TESTS <=====================");
    }

    private IngestRequest createIngestRequest(AIPEntity aipEntity) {
        IngestRequest ingestRequest = IngestRequest.build(null,
                                                          IngestMetadata.build("SESSION_OWNER",
                                                                               "SESSION",
                                                                               null,
                                                                               "ingestChain",
                                                                               new HashSet<>(),
                                                                               StorageMetadata.build("RAS")),
                                                          InternalRequestState.CREATED,
                                                          IngestRequestStep.LOCAL_SCHEDULED,
                                                          aipEntity.getSip().getSip());
        ingestRequest.setAips(Lists.newArrayList(aipEntity));
        return ingestRequestRepository.save(ingestRequest);
    }

    private OAISDeletionRequest createOAISDeletionRequest(List<AIPEntity> aips) {
        OAISDeletionRequest oaisDeletionRequest = OAISDeletionRequest.build(aips.get(0),
                                                                            SessionDeletionMode.BY_STATE,
                                                                            true);
        return (OAISDeletionRequest) requestService.scheduleRequest(oaisDeletionRequest);
    }

    private OAISDeletionCreatorRequest createOAISDeletionCreatorRequest() {
        OAISDeletionCreatorRequest deletionRequest = OAISDeletionCreatorRequest.build(new OAISDeletionCreatorPayload());
        return (OAISDeletionCreatorRequest) requestService.scheduleRequest(deletionRequest);
    }

    private List<AIPUpdateRequest> createUpdateRequest(List<AIPEntity> aips) {
        List<AIPUpdateRequest> updateRequests = AIPUpdateRequest.build(aips.get(0),
                                                                       AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(
                                                                                                 SESSION_0))
                                                                                             .withAddTags(Lists.newArrayList(
                                                                                                 "SOME TAG")),
                                                                       false);
        List<AbstractRequest> list = new ArrayList<>();
        for (AIPUpdateRequest ur : updateRequests) {
            list.add(ur);
        }
        requestService.scheduleRequests(list);
        return updateRequests;
    }

    private AIPUpdatesCreatorRequest createAIPUpdatesCreatorRequest() {
        AIPUpdatesCreatorRequest updateCreatorRequest = AIPUpdatesCreatorRequest.build(AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(
            SESSION_0)));
        return (AIPUpdatesCreatorRequest) requestService.scheduleRequest(updateCreatorRequest);
    }

    private AIPPostProcessRequest createPostProcessRequest(AIPEntity aip) {
        AIPPostProcessRequest postProcessRequest = AIPPostProcessRequest.build(aip, "sampleId");
        return (AIPPostProcessRequest) requestService.scheduleRequest(postProcessRequest);
    }

    public void clearRequest() {
        ingestServiceTest.waitDuring(1000);
        abstractRequestRepository.deleteAll();
        LOGGER.info("Entities still existing count : {} ", abstractRequestRepository.count());

        LOGGER.info("Jobs stil existing : {}", jobInfoRepository.count());
        LOGGER.info("Let's remove {} jobs info", jobInfoRepository.count());
        jobInfoRepository.deleteAll();

    }

    @Test
    public void testScheduleRequest() {
        clearRequest();
        prepareOAISEntities();

        // BEGIN ------ Empty repo tests
        IngestRequest ingestRequest = createIngestRequest(aips.get(0));
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            ingestRequest.getState());
        clearRequest();

        AIPUpdatesCreatorRequest aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            aipUpdatesCreatorRequest.getState());
        clearRequest();

        List<AIPUpdateRequest> updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, request.getState());
        }
        clearRequest();

        OAISDeletionCreatorRequest oaisDeletionRequest = createOAISDeletionCreatorRequest();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            oaisDeletionRequest.getState());
        clearRequest();

        OAISDeletionRequest storageDeletionRequest = createOAISDeletionRequest(aips);
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            storageDeletionRequest.getState());
        clearRequest();

        AIPPostProcessRequest aipPostProcessRequest = createPostProcessRequest(aips.get(0));
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            aipPostProcessRequest.getState());
        clearRequest();

        // END  ------ Empty repo tests

        // BEGIN ------- Test AIPUpdatesCreatorRequest
        createIngestRequest(aips.get(0));
        createAIPUpdatesCreatorRequest();
        createUpdateRequest(aips);
        createOAISDeletionRequest(aips);

        aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            aipUpdatesCreatorRequest.getState());
        clearRequest();
        // END ------- Test AIPUpdatesCreatorRequest

        // BEGIN ------- Test AIPUpdateRequest
        createIngestRequest(aips.get(0));
        createAIPUpdatesCreatorRequest();
        updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should not be blocked", InternalRequestState.CREATED, request.getState());
        }
        clearRequest();
        // END ------- Test AIPUpdateRequest

        // AIPStoreMetaDataRequest does not deserve more tests

        // BEGIN ------- Test OAISDeletionRequest
        createIngestRequest(aips.get(0));
        createOAISDeletionRequest(aips);
        oaisDeletionRequest = createOAISDeletionCreatorRequest();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            oaisDeletionRequest.getState());
        clearRequest();
        // END ------- Test OAISDeletionRequest

        // BEGIN ------- Test StorageDeletionRequest
        createIngestRequest(aips.get(0));
        createOAISDeletionCreatorRequest();
        storageDeletionRequest = createOAISDeletionRequest(aips);
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            storageDeletionRequest.getState());
        clearRequest();
        // END ------- Test StorageDeletionRequest

        // BEGIN ------- Test AIPPostProcessRequest
        createIngestRequest(aips.get(0));
        aipPostProcessRequest = createPostProcessRequest(aips.get(0));
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            aipPostProcessRequest.getState());
        clearRequest();
        // END ------- Test AIPPostProcessRequest
    }

    @Test
    public void testScheduleAsBlockRequest() {
        clearRequest();
        prepareOAISEntities();

        // BEGIN ------- Test AIPUpdatesCreatorRequest
        createOAISDeletionCreatorRequest();
        AIPUpdatesCreatorRequest aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        Assert.assertEquals("The request should be blocked",
                            InternalRequestState.BLOCKED,
                            aipUpdatesCreatorRequest.getState());
        clearRequest();
        // END ------- Test AIPUpdatesCreatorRequest

        // BEGIN ------- Test AIPUpdateRequest
        createOAISDeletionRequest(aips);
        List<AIPUpdateRequest> updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, request.getState());
        }
        clearRequest();

        createOAISDeletionCreatorRequest();
        updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, request.getState());
        }
        clearRequest();

        createPostProcessRequest(aips.get(0));
        updateRequest = createUpdateRequest(aips);
        for (AIPUpdateRequest request : updateRequest) {
            Assert.assertEquals("The request should be blocked", InternalRequestState.BLOCKED, request.getState());
        }
        clearRequest();
        // END ------- Test AIPUpdateRequest

        // BEGIN ------- Test OAISDeletionCreatorRequest
        createUpdateRequest(aips);
        OAISDeletionCreatorRequest oaisDeletionRequest = createOAISDeletionCreatorRequest();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.BLOCKED,
                            oaisDeletionRequest.getState());
        clearRequest();
        // END ------- Test OAISDeletionCreatorRequest

        // BEGIN ------- Test StorageDeletionRequest
        createUpdateRequest(aips);
        OAISDeletionRequest storageDeletionRequest = createOAISDeletionRequest(aips);
        Assert.assertEquals("The request should be blocked",
                            InternalRequestState.BLOCKED,
                            storageDeletionRequest.getState());
        clearRequest();

        createAIPUpdatesCreatorRequest();
        storageDeletionRequest = createOAISDeletionRequest(aips);
        Assert.assertEquals("The request should be blocked",
                            InternalRequestState.BLOCKED,
                            storageDeletionRequest.getState());
        clearRequest();

        createPostProcessRequest(aips.get(0));
        storageDeletionRequest = createOAISDeletionRequest(aips);
        Assert.assertEquals("The request should be blocked",
                            InternalRequestState.BLOCKED,
                            storageDeletionRequest.getState());
        clearRequest();
        // END ------- Test StorageDeletionRequest

    }

    @Test
    public void testUnblockAIPUpdatesCreatorMacro() {
        clearRequest();

        prepareOAISEntities();
        AIPUpdatesCreatorRequest aipUpdatesCreatorRequest = createAIPUpdatesCreatorRequest();
        aipUpdatesCreatorRequest.setState(InternalRequestState.BLOCKED);
        aipUpdatesCreatorRequest = abstractRequestRepository.save(aipUpdatesCreatorRequest);
        Assert.assertEquals("The request should be blocked",
                            InternalRequestState.BLOCKED,
                            aipUpdatesCreatorRequest.getState());

        requestService.unblockRequests(RequestTypeEnum.AIP_UPDATES_CREATOR);
        aipUpdatesCreatorRequest = (AIPUpdatesCreatorRequest) abstractRequestRepository.findById(
            aipUpdatesCreatorRequest.getId()).get();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.RUNNING,
                            aipUpdatesCreatorRequest.getState());
    }

    @Test
    public void testUnblockStorageDeletionMicro() {
        clearRequest();

        prepareOAISEntities();
        OAISDeletionRequest oaisDeletionRequest = createOAISDeletionRequest(aips);
        oaisDeletionRequest.setState(InternalRequestState.BLOCKED);
        oaisDeletionRequest = abstractRequestRepository.save(oaisDeletionRequest);
        Assert.assertEquals("The request should be blocked",
                            InternalRequestState.BLOCKED,
                            oaisDeletionRequest.getState());

        requestService.unblockRequests(RequestTypeEnum.OAIS_DELETION);
        oaisDeletionRequest = (OAISDeletionRequest) abstractRequestRepository.findById(oaisDeletionRequest.getId())
                                                                             .get();
        Assert.assertEquals("The request should not be blocked",
                            InternalRequestState.CREATED,
                            oaisDeletionRequest.getState());
    }

}
