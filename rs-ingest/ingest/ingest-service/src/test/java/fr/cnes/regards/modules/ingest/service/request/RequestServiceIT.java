/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdatesCreatorRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IOAISDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IStorageDeletionRequestRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.deletion.OAISDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.deletion.StorageDeletionRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdatesCreatorRequest;
import fr.cnes.regards.modules.ingest.dto.aip.SearchAIPsParameters;
import fr.cnes.regards.modules.ingest.dto.request.RequestDto;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.dto.request.SessionDeletionMode;
import fr.cnes.regards.modules.ingest.dto.request.update.AIPUpdateParametersDto;
import fr.cnes.regards.modules.ingest.dto.sip.flow.IngestRequestFlowItem;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.storagelight.client.test.StorageClientMock;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(
    properties = { "spring.jpa.properties.hibernate.default_schema=request_it",
        "regards.aips.save-metadata.bulk.delay=20000000",  "regards.amqp.enabled=true",
        "eureka.client.enabled=false", "regards.scheduler.pool.size=4", "regards.ingest.maxBulkSize=100",
            "spring.jpa.show-sql=true"
    }
)
@ActiveProfiles(value={"testAmqp", "StorageClientMock"})
public class RequestServiceIT extends IngestMultitenantServiceTest {

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
    private IAIPStoreMetaDataRepository storeMetaDataRepository;

    @Autowired
    private IAIPUpdatesCreatorRepository aipUpdatesCreatorRepository;

    @Autowired
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Autowired
    private IIngestRequestRepository ingestRequestRepository;

    @Autowired
    private IOAISDeletionRequestRepository oaisDeletionRequestRepository;

    @Autowired
    private IStorageDeletionRequestRepository storageDeletionRequestRepository;

    @Autowired
    private ISubscriber subscriber;


    @Autowired
    private StorageClientMock storageClient;

    @Override
    public void doInit() {
        simulateApplicationReadyEvent();
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    @Override
    protected void doAfter() throws Exception {
        // WARNING : clean context manually because Spring doesn't do it between tests
        subscriber.unsubscribeFrom(IngestRequestFlowItem.class);
    }

    public void initData() {
        storageClient.setBehavior(true, true);
        long nbSIP = 7;
        publishSIPEvent(create("provider 1", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 2", TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 3", TAG_1), STORAGE_1, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 4", TAG_1), STORAGE_1, SESSION_1, SESSION_OWNER_1, CATEGORIES_1);
        publishSIPEvent(create("provider 5", TAG_1), STORAGE_2, SESSION_1, SESSION_OWNER_1, CATEGORIES_2);
        publishSIPEvent(create("provider 6", TAG_0), STORAGE_2, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        publishSIPEvent(create("provider 7", TAG_2), STORAGE_0, SESSION_1, SESSION_OWNER_0, CATEGORIES_0);
        // Wait
        ingestServiceTest.waitForIngestion(nbSIP, nbSIP * 1000);

        List<AIPEntity> aips = aipRepository.findAll();

        // Create an event of each type and ensure they are not consummed by jobs / queue / whatever
        AIPStoreMetaDataRequest storeMetaDataRequest = AIPStoreMetaDataRequest.build(aips.get(0), true, true);
        storeMetaDataRequest.setState(InternalRequestStep.ERROR);
        storeMetaDataRepository.save(storeMetaDataRequest);

        AIPUpdatesCreatorRequest updateCreatorRequest = AIPUpdatesCreatorRequest.build(
                AIPUpdateParametersDto.build(SearchAIPsParameters.build().withSession(SESSION_0)));
        updateCreatorRequest.setState(InternalRequestStep.ERROR);
        aipUpdatesCreatorRepository.save(updateCreatorRequest);

        List<AIPUpdateRequest> updateRequest = AIPUpdateRequest.build(
                aips.get(0),
                AIPUpdateParametersDto.build(SearchAIPsParameters.build().withSession(SESSION_0)).withAddTags(Lists.newArrayList("SOME TAG")),
                true
        );
        updateRequest.get(0).setState(InternalRequestStep.ERROR);
        aipUpdateRequestRepository.saveAll(updateRequest);

        ingestRequestRepository.save(IngestRequest.build(aips.get(0).getIngestMetadata(), InternalRequestStep.ERROR,
                IngestRequestStep.REMOTE_STORAGE_ERROR, aips.get(0).getSip().getSip()));
        OAISDeletionRequest deletionRequest = new OAISDeletionRequest();
        deletionRequest.setCreationDate(OffsetDateTime.now());
        deletionRequest.setState(InternalRequestStep.ERROR);
        oaisDeletionRequestRepository.save(deletionRequest);

        StorageDeletionRequest storageDeletionRequest = StorageDeletionRequest.build("some request id",
                aips.get(0).getSip(), SessionDeletionMode.BY_STATE);
        storageDeletionRequest.setState(InternalRequestStep.ERROR);
        storageDeletionRequestRepository.save(storageDeletionRequest);
    }



    @Test
    public void testSearchRequests() throws ModuleException {
        initData();
        PageRequest pr = PageRequest.of(0, 100);
        Page<RequestDto> requests = requestService.searchRequests(SearchRequestsParameters.build().withState(InternalRequestStep.ERROR), pr);
        Assert.assertEquals(6, requests.getTotalElements());



        requests = requestService.searchRequests(
                SearchRequestsParameters.build().withRequestType(RequestTypeEnum.INGEST).withState(InternalRequestStep.ERROR), pr);
        Assert.assertEquals(1, requests.getTotalElements());


        requests = requestService.searchRequests(
                SearchRequestsParameters.build().withRequestType(RequestTypeEnum.AIP_UPDATES_CREATOR).withState(InternalRequestStep.ERROR), pr);
        Assert.assertEquals(1, requests.getTotalElements());

        requests = requestService.searchRequests(
                SearchRequestsParameters.build().withRequestType(RequestTypeEnum.OAIS_DELETION).withState(InternalRequestStep.ERROR), pr);
        Assert.assertEquals(1, requests.getTotalElements());

        requests = requestService.searchRequests(
                SearchRequestsParameters.build().withRequestType(RequestTypeEnum.STORAGE_DELETION).withState(InternalRequestStep.ERROR), pr);
        Assert.assertEquals(1, requests.getTotalElements());

        requests = requestService.searchRequests(
                SearchRequestsParameters.build().withRequestType(RequestTypeEnum.STORE_METADATA).withState(InternalRequestStep.ERROR)
                , pr);
        Assert.assertEquals(1, requests.getTotalElements());

        requests = requestService.searchRequests(
                SearchRequestsParameters.build().withRequestType(RequestTypeEnum.UPDATE).withState(InternalRequestStep.ERROR), pr);
        Assert.assertEquals(1, requests.getTotalElements());
    }
}
