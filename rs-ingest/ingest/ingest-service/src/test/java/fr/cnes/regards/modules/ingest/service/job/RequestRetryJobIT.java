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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.*;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
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
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=request_retry_job", "regards.amqp.enabled=true",
        "regards.ingest.aip.update.bulk.delay=100000000", "eureka.client.enabled=false",
        "regards.ingest.aip.delete.bulk.delay=100" }, locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class RequestRetryJobIT extends IngestMultitenantServiceIT {

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

        sip4.setSip(SIP.build(EntityType.DATA, "SIP_001").withDescriptiveInformation("version", "2"));
        sip4.setSipId(OaisUniformResourceName.fromString("URN:SIP:COLLECTION:DEFAULT:"
                                                         + UUID.randomUUID().toString()
                                                         + ":V1"));
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

        AIP aip = AIP.build(sip4.getSip(),
                            OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 1),
                            Optional.empty(),
                            "SIP_001",
                            sip4.getVersion());
        aip.setIpType(EntityType.DATA);
        AIPEntity aipEntity = AIPEntity.build(sip4, AIPState.GENERATED, aip);

        aipEntity = aipRepository.save(aipEntity);

        AIP aip2 = AIP.build(sip4.getSip(),
                             OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 1),
                             Optional.empty(),
                             "SIP_002",
                             sip4.getVersion());
        AIPEntity aipEntity2 = AIPEntity.build(sip4, AIPState.GENERATED, aip2);

        aipEntity2 = aipRepository.save(aipEntity2);

        mtd = IngestMetadataDto.build(SESSION_OWNER_0,
                                      SESSION_0,
                                      IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                      Sets.newHashSet(CATEGORIES_0),
                                      null,
                                      StorageMetadata.build(STORAGE_0));

        aips = aipRepository.findAll();

        // Create an event of each type and ensure they are not consummed by jobs / queue / whatever
        AIPUpdatesCreatorRequest updateCreatorRequest = AIPUpdatesCreatorRequest.build(AIPUpdateParametersDto.build(
            SearchAIPsParameters.build().withSession(SESSION_0).withSessionOwner(SESSION_OWNER_0)));
        updateCreatorRequest.setState(InternalRequestState.ERROR);
        aipUpdatesCreatorRepository.save(updateCreatorRequest);

        List<AIPUpdateRequest> updateRequest = AIPUpdateRequest.build(aips.get(0),
                                                                      AIPUpdateParametersDto.build(SearchAIPsParameters.build()
                                                                                                                       .withSession(
                                                                                                                           SESSION_0))
                                                                                            .withAddTags(Lists.newArrayList(
                                                                                                "SOME TAG")),
                                                                      true);
        updateRequest.get(0).setState(InternalRequestState.ERROR);
        aipUpdateRequestRepository.saveAll(updateRequest);

        IngestRequest ir = IngestRequest.build(null,
                                               mapper.dtoToMetadata(mtd),
                                               InternalRequestState.ERROR,
                                               IngestRequestStep.REMOTE_STORAGE_ERROR,
                                               aips.get(0).getSip().getSip());
        ir.setAips(aips);
        ingestRequestRepository.save(ir);
        OAISDeletionCreatorRequest deletionRequest = new OAISDeletionCreatorRequest();
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
     * @throws InterruptedException
     */
    public void waitForErrorRequestReach(long expectedTasks, long timeout) {
        long end = System.currentTimeMillis() + timeout;
        // Wait
        long errorRequestCount;
        do {
            Pageable unpaged = Pageable.unpaged();
            errorRequestCount = abstractRequestRepository.findAll(AbstractRequestSpecifications.searchAllByFilters(
                SearchRequestsParameters.build().withState(InternalRequestState.ERROR),
                unpaged), unpaged).getTotalElements();
            LOGGER.info("{} UpdateRequest(s) existing in database", errorRequestCount);
            if (errorRequestCount == expectedTasks) {
                break;
            }
            long now = System.currentTimeMillis();
            if (end > now) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Assert.fail("Thread interrupted");
                }
            } else {
                Assert.fail("Timeout");
            }
        } while (true);
    }

    @Test
    public void testRetryJob() {
        initData();
        Assert.assertEquals("Something went wrong while creating requests", 5, abstractRequestRepository.count());
        requestService.scheduleRequestRetryJob(SearchRequestsParameters.build()
                                                                       .withRequestType(RequestTypeEnum.AIP_UPDATES_CREATOR));
        waitForErrorRequestReach(5, 20_000);

        requestService.scheduleRequestRetryJob(SearchRequestsParameters.build()
                                                                       .withSession(SESSION_0)
                                                                       .withSessionOwner(SESSION_OWNER_0));
        waitForErrorRequestReach(1, 10_000 * 5);

        requestService.scheduleRequestRetryJob(SearchRequestsParameters.build());
        waitForErrorRequestReach(0, 10_000);
    }

}
