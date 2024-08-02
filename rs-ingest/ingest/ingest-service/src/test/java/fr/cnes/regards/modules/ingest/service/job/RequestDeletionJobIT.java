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
package fr.cnes.regards.modules.ingest.service.job;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.*;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.mapper.IIngestMetadataMapper;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.StorageDto;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestState;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.event.IngestRequestEvent;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_deletion_job",
                                   "regards.amqp.enabled=true",
                                   "regards.ingest.aip.update.bulk.delay=100000000",
                                   "eureka.client.enabled=false",
                                   "spring.jpa.show-sql=false",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class RequestDeletionJobIT extends IngestMultitenantServiceIT {

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final String STORAGE_0 = "fake";

    private static final String SESSION_OWNER_0 = "NASA";

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    private IOAISDeletionCreatorRepository oaisDeletionCreatorRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Autowired
    private IIngestMetadataMapper mapper;

    private List<AIPEntity> aips;

    private IngestMetadataDto mtd;

    public void initData() {
        LOGGER.info("=========================> BEGIN INIT DATA FOR TESTS <=====================");
        SIPEntity sip4 = new SIPEntity();

        sip4.setSip(SIPDto.build(EntityType.DATA, "SIP_001").withDescriptiveInformation("version", "2"));
        sip4.setSipId(OaisUniformResourceName.fromString("URN:SIP:COLLECTION:DEFAULT:" + UUID.randomUUID() + ":V1"));
        sip4.setProviderId("SIP_003");
        sip4.setCreationDate(OffsetDateTime.now().minusHours(6));
        sip4.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sip4.setSessionOwner(SESSION_OWNER_0);
        sip4.setSession(SESSION_0);
        sip4.setCategories(org.assertj.core.util.Sets.newLinkedHashSet("CATEGORIES"));
        sip4.setState(SIPState.INGESTED);
        sip4.setVersion(2);
        sip4.setChecksum("123456789032");

        sip4 = sipRepository.save(sip4);

        AIPDto aip = AIPDto.build(sip4.getSip(),
                                  OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                          EntityType.DATA,
                                                                          "tenant",
                                                                          1),
                                  Optional.empty(),
                                  "SIP_001",
                                  sip4.getVersion());
        aip.setIpType(EntityType.DATA);
        AIPEntity aipEntity = AIPEntity.build(sip4, AIPState.GENERATED, aip);

        aipEntity = aipRepository.save(aipEntity);

        AIPDto aip2 = AIPDto.build(sip4.getSip(),
                                   OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                           EntityType.DATA,
                                                                           "tenant",
                                                                           1),
                                   Optional.empty(),
                                   "SIP_002",
                                   sip4.getVersion());
        aip2.setVersion(sip4.getVersion());
        AIPEntity aipEntity2 = AIPEntity.build(sip4, AIPState.GENERATED, aip2);

        aipEntity2 = aipRepository.save(aipEntity2);

        mtd = new IngestMetadataDto(SESSION_OWNER_0,
                                    SESSION_0,
                                    null,
                                    IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                    Sets.newHashSet(CATEGORIES_0),
                                    null,
                                    null,
                                    new StorageDto(STORAGE_0));

        aips = aipRepository.findAll();

        // Create an event of each type and ensure they are not consummed by jobs / queue / whatever
        AIPUpdatesCreatorRequest updateCreatorRequest = AIPUpdatesCreatorRequest.build(AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(
            SESSION_0).withSessionOwner(SESSION_OWNER_0)));
        updateCreatorRequest.setState(InternalRequestState.ERROR);
        aipUpdatesCreatorRepository.save(updateCreatorRequest);

        List<AIPUpdateRequest> updateRequest = AIPUpdateRequest.build(aips.get(0),
                                                                      AIPUpdateParametersDto.build(new SearchAIPsParameters().withSession(
                                                                                                SESSION_0))
                                                                                            .withAddTags(Lists.newArrayList(
                                                                                                "SOME TAG")),
                                                                      true);
        updateRequest.get(0).setState(InternalRequestState.ERROR);
        aipUpdateRequestRepository.saveAll(updateRequest);

        IngestRequest ingestRequest = IngestRequest.build(null,
                                                          mapper.dtoToMetadata(mtd),
                                                          InternalRequestState.ERROR,
                                                          IngestRequestStep.REMOTE_STORAGE_ERROR,
                                                          aips.get(0).getSip().getSip());
        ingestRequest.setAips(List.of(aips.get(0)));
        ingestRequestRepository.save(ingestRequest);
        OAISDeletionCreatorRequest deletionRequest = new OAISDeletionCreatorRequest(UUID.randomUUID().toString());
        deletionRequest.setCreationDate(OffsetDateTime.now());
        deletionRequest.setState(InternalRequestState.ERROR);
        oaisDeletionCreatorRepository.save(deletionRequest);

        OAISDeletionRequest oaisDeletionRequest = OAISDeletionRequest.build(aips.get(0),
                                                                            SessionDeletionMode.BY_STATE,
                                                                            true);
        oaisDeletionRequest.setState(InternalRequestState.ERROR);
        oaisDeletionRequestRepository.save(oaisDeletionRequest);
        LOGGER.info("=========================> END INIT DATA FOR TESTS <=====================");
    }

    /**
     * Helper method to wait for DB ingestion
     *
     * @param expectedTasks expected count of task in db
     * @param timeout       in ms
     */
    public void waitForRequestReach(long expectedTasks, long timeout, String tenant) {
        Awaitility.await().atMost(timeout, TimeUnit.MILLISECONDS).until(() -> {
            runtimeTenantResolver.forceTenant(tenant);
            return abstractRequestRepository.count() == expectedTasks;
        });
    }

    @Test
    public void testDeleteJob() {
        initData();
        Assert.assertEquals("Something went wrong while creating requests", 5, abstractRequestRepository.count());
        requestService.scheduleRequestDeletionJob(new SearchRequestParameters().withRequestIpTypesIncluded(Set.of(
            RequestTypeEnum.AIP_UPDATES_CREATOR)));
        waitForRequestReach(5, 20_000, getDefaultTenant());

        Assert.assertEquals(2L, aipRepository.count());
        Assert.assertEquals(1L, sipRepository.count());
        requestService.scheduleRequestDeletionJob(new SearchRequestParameters().withSession(SESSION_0)
                                                                               .withSessionOwner(SESSION_OWNER_0));
        waitForRequestReach(1, 10_000, getDefaultTenant());
        Assert.assertEquals("One AIP should have been deleted.", 1L, aipRepository.count());
        Assert.assertEquals("One SIP should have been deleted.", 1L, sipRepository.count());

        requestService.scheduleRequestDeletionJob(new SearchRequestParameters());
        waitForRequestReach(0, 10_000, getDefaultTenant());

        List<IngestRequestEvent> events = getPublishedEvents(IngestRequestEvent.class);
        Assert.assertEquals("There sould be one ingestion event sent to inform deletion", 1, events.size());
        Assert.assertEquals("The ingest event should be deletion information event",
                            RequestState.DELETED,
                            events.get(0).getState());
    }

}
