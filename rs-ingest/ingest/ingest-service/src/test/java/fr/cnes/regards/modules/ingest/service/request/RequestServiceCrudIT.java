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
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
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
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.*;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=request_crud_it", "regards.amqp.enabled=true",
        "eureka.client.enabled=false", "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100",
        "spring.jpa.show-sql=false" }, locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock", "noscheduler" })
public class RequestServiceCrudIT extends IngestMultitenantServiceIT {

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> CATEGORIES_1 = Lists.newArrayList("CATEGORY1");

    private static final List<String> CATEGORIES_2 = Lists.newArrayList("CATEGORY", "CATEGORY2");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("antonio", "farra's");

    private static final String STORAGE_0 = "fake";

    private static final String STORAGE_1 = "AWS";

    private static final String STORAGE_2 = "Azure";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String SESSION_OWNER_1 = "CNES";

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    public static final String SESSION_1 = OffsetDateTime.now().minusDays(4).toString();

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

    @Autowired
    private StorageClientMock storageClient;

    public void initData() {
        LOGGER.info("=========================> BEGIN INIT DATA FOR TESTS <=====================");
        storageClient.setBehavior(true, true);
        long nbSIP = 7;
        publishSIPEvent(create("provider 1", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 2", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 3", TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_2);
        publishSIPEvent(create("provider 6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 7", TAG_2), STORAGE_0, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);

        IngestMetadataDto mtd = IngestMetadataDto.build(SESSION_OWNER_0,
                                                        SESSION_0,
                                                        IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                        Sets.newHashSet(CATEGORIES_0),
                                                        null,
                                                        StorageMetadata.build(STORAGE_0));
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 1000);

        List<AIPEntity> aips = aipRepository.findAll();

        // Create an event of each type and ensure they are not consummed by jobs / queue / whatever
        AIPUpdatesCreatorRequest updateCreatorRequest = AIPUpdatesCreatorRequest.build(AIPUpdateParametersDto.build(
            SearchAIPsParameters.build().withSession(SESSION_0)));
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

        ingestRequestRepository.save(IngestRequest.build(null,
                                                         mapper.dtoToMetadata(mtd),
                                                         InternalRequestState.ERROR,
                                                         IngestRequestStep.REMOTE_STORAGE_ERROR,
                                                         aips.get(0).getSip().getSip()));
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

    @Test
    public void testSearchRequests() throws ModuleException {
        initData();
        PageRequest pr = PageRequest.of(0, 100);
        LOGGER.info("=========================> BEGIN SEARCH ALL IN ERROR <=====================");
        Page<RequestDto> requests = requestService.findRequestDtos(SearchRequestsParameters.build()
                                                                                           .withState(
                                                                                               InternalRequestState.ERROR),
                                                                   pr);
        SearchAbstractRequestParameters searchAbstractRequestParameters = new SearchAbstractRequestParameters().withRequestStatesIncluded(
            Arrays.asList(InternalRequestState.ERROR));
        Page<RequestDto> requestDtos = requestService.findRequestDtos(searchAbstractRequestParameters, pr);
        LOGGER.info("=========================> END SEARCH ALL IN ERROR <=====================");
        Assert.assertEquals(5, requests.getTotalElements());
        Assert.assertEquals(5, requestDtos.getTotalElements());

        LOGGER.info("=========================> BEGIN SEARCH INGEST IN ERROR <=====================");

        requests = requestService.findRequestDtos(SearchRequestsParameters.build()
                                                                          .withRequestType(RequestTypeEnum.INGEST)
                                                                          .withState(InternalRequestState.ERROR), pr);

        searchAbstractRequestParameters = new SearchAbstractRequestParameters().withRequestIpTypesIncluded(Arrays.asList(
            RequestTypeEnum.INGEST)).withRequestStatesIncluded(Arrays.asList(InternalRequestState.ERROR));
        requestDtos = requestService.findRequestDtos(searchAbstractRequestParameters, pr);

        LOGGER.info("=========================> END SEARCH INGEST IN ERROR <=====================");
        Assert.assertEquals(1, requests.getTotalElements());
        Assert.assertEquals(1, requestDtos.getTotalElements());

        LOGGER.info("=========================> BEGIN SEARCH AIP UPDATE CREATOR IN ERROR <=====================");
        requests = requestService.findRequestDtos(SearchRequestsParameters.build()
                                                                          .withRequestType(RequestTypeEnum.AIP_UPDATES_CREATOR)
                                                                          .withState(InternalRequestState.ERROR), pr);
        searchAbstractRequestParameters = new SearchAbstractRequestParameters().withRequestIpTypesIncluded(Arrays.asList(
            RequestTypeEnum.AIP_UPDATES_CREATOR)).withRequestStatesIncluded(Arrays.asList(InternalRequestState.ERROR));
        requestDtos = requestService.findRequestDtos(searchAbstractRequestParameters, pr);
        LOGGER.info("=========================> END SEARCH AIP UPDATE CREATOR IN ERROR <=====================");
        Assert.assertEquals(1, requests.getTotalElements());
        Assert.assertEquals(1, requestDtos.getTotalElements());

        LOGGER.info("=========================> BEGIN SEARCH OAIS DELETION CREATOR IN ERROR <=====================");
        requests = requestService.findRequestDtos(SearchRequestsParameters.build()
                                                                          .withRequestType(RequestTypeEnum.OAIS_DELETION_CREATOR)
                                                                          .withState(InternalRequestState.ERROR), pr);
        searchAbstractRequestParameters = new SearchAbstractRequestParameters().withRequestIpTypesIncluded(Arrays.asList(
                                                                                   RequestTypeEnum.OAIS_DELETION_CREATOR))
                                                                               .withRequestStatesIncluded(Arrays.asList(
                                                                                   InternalRequestState.ERROR));
        requestDtos = requestService.findRequestDtos(searchAbstractRequestParameters, pr);
        LOGGER.info("=========================> END SEARCH OAIS DELETION CREATOR IN ERROR <=====================");
        Assert.assertEquals(1, requests.getTotalElements());
        Assert.assertEquals(1, requestDtos.getTotalElements());

        LOGGER.info("=========================> BEGIN SEARCH STORAGE DELETION IN ERROR <=====================");
        requests = requestService.findRequestDtos(SearchRequestsParameters.build()
                                                                          .withRequestType(RequestTypeEnum.OAIS_DELETION)
                                                                          .withState(InternalRequestState.ERROR), pr);
        searchAbstractRequestParameters = new SearchAbstractRequestParameters().withRequestIpTypesIncluded(Arrays.asList(
            RequestTypeEnum.OAIS_DELETION)).withRequestStatesIncluded(Arrays.asList(InternalRequestState.ERROR));
        requestDtos = requestService.findRequestDtos(searchAbstractRequestParameters, pr);
        LOGGER.info("=========================> END SEARCH STORAGE DELETION IN ERROR <=====================");
        Assert.assertEquals(1, requests.getTotalElements());
        Assert.assertEquals(1, requestDtos.getTotalElements());

        LOGGER.info("=========================> BEGIN SEARCH UPDATE IN ERROR <=====================");
        requests = requestService.findRequestDtos(SearchRequestsParameters.build()
                                                                          .withRequestType(RequestTypeEnum.UPDATE)
                                                                          .withState(InternalRequestState.ERROR), pr);
        searchAbstractRequestParameters = new SearchAbstractRequestParameters().withRequestIpTypesIncluded(Arrays.asList(
            RequestTypeEnum.UPDATE)).withRequestStatesIncluded(Arrays.asList(InternalRequestState.ERROR));
        requestDtos = requestService.findRequestDtos(searchAbstractRequestParameters, pr);
        LOGGER.info("=========================> END SEARCH UPDATE IN ERROR <=====================");
        Assert.assertEquals(1, requests.getTotalElements());
        Assert.assertEquals(1, requestDtos.getTotalElements());
    }

    public HashSet<AIPEntity> makeRequests() {

        SIPEntity sip4 = new SIPEntity();

        sip4.setSip(SIP.build(EntityType.DATA, "SIP_001").withDescriptiveInformation("version", "2"));
        sip4.setSipId(OaisUniformResourceName.fromString("URN:SIP:COLLECTION:DEFAULT:"
                                                         + UUID.randomUUID().toString()
                                                         + ":V1"));
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

        IngestRequest ingestRequest = IngestRequest.build(null,
                                                          IngestMetadata.build("SESSION_OWNER",
                                                                               "SESSION",
                                                                               "ingestChain",
                                                                               new HashSet<>(),
                                                                               StorageMetadata.build("RAS")),
                                                          InternalRequestState.ERROR,
                                                          IngestRequestStep.LOCAL_SCHEDULED,
                                                          aipEntity.getSip().getSip());
        ingestRequest.setAips(Lists.newArrayList(aipEntity));
        abstractRequestRepository.save(ingestRequest);

        List<AIPUpdateRequest> updateRequest = AIPUpdateRequest.build(aipEntity2,
                                                                      AIPUpdateParametersDto.build(SearchAIPsParameters.build())
                                                                                            .withAddTags(Lists.newArrayList(
                                                                                                "TEST")),
                                                                      false);
        updateRequest.get(0).setState(InternalRequestState.ERROR);
        abstractRequestRepository.saveAll(updateRequest);
        return Sets.newHashSet(aipEntity, aipEntity2);
    }

    @Test
    public void testDeleteRequestByAip() {
        Set<AIPEntity> aipEntities = makeRequests();
        Assert.assertEquals(2, abstractRequestRepository.count());
        // Delete all requests associated to AIP
        requestService.deleteAllByAip(aipEntities);
        Assert.assertEquals(0, abstractRequestRepository.count());
        // Now we can delete the AIP
        aipRepository.deleteAll(aipEntities);
    }
}