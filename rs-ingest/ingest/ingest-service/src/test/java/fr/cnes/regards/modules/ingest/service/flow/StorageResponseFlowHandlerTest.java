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
package fr.cnes.regards.modules.ingest.service.flow;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAIPStoreMetaDataRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.request.manifest.AIPStoreMetaDataRequest;
import fr.cnes.regards.modules.ingest.domain.request.manifest.StoreLocation;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestsParameters;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceTest;
import fr.cnes.regards.modules.ingest.service.flow.StorageResponseFlowHandler;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.storage.client.RequestInfo;
import fr.cnes.regards.modules.storage.domain.dto.FileLocationDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceDTO;
import fr.cnes.regards.modules.storage.domain.dto.FileReferenceMetaInfoDTO;
import fr.cnes.regards.modules.storage.domain.dto.request.RequestResultInfoDTO;

/**
 * @author sbinda
 *
 */
@ActiveProfiles({ "noscheduler" })
@TestPropertySource(
        properties = { "spring.jpa.show-sql=false",
                "spring.jpa.properties.hibernate.default_schema=ingest_request_tests" },
        locations = { "classpath:application-test.properties" })
public class StorageResponseFlowHandlerTest extends IngestMultitenantServiceTest {

    @Autowired
    private StorageResponseFlowHandler storageResponseFlowHandler;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IIngestRequestRepository ingestReqRepo;

    @Autowired
    private IAIPStoreMetaDataRepository storeMetaReqRepo;

    @Autowired
    private ISIPRepository sipRepo;

    @Autowired
    private IAIPRepository aipRepo;

    private Set<RequestInfo> initAips(int nbAips, boolean metaStorage) {
        Set<RequestInfo> rq = Sets.newHashSet();
        // Init 1000 sip/aip waiting for storage responses
        for (int i = 0; i < nbAips; i++) {
            rq.add(initAip(UUID.randomUUID().toString(), "aip_number_" + i, metaStorage));
        }
        return rq;
    }

    private RequestInfo initAip(String fileToStoreChecksum, String providerId, boolean metaStorage) {
        String sessionOwner = "sessionOwner";
        String session = "session";
        String storage = "storage";
        String storePath = null;
        MimeType mimeType = MediaType.APPLICATION_JSON_UTF8;
        SIP sip = SIP.build(EntityType.DATA, providerId);
        SIPEntity sipEntity = SIPEntity.build(getDefaultTenant(),
                                              IngestMetadata.build(sessionOwner, session, "ingestChain",
                                                                   Sets.newHashSet(), StorageMetadata.build(storage)),
                                              sip, 1, metaStorage ? SIPState.STORED : SIPState.INGESTED);
        sipEntity.setChecksum(UUID.randomUUID().toString());
        sipEntity.setLastUpdate(OffsetDateTime.now());
        sipEntity = sipRepo.save(sipEntity);
        UniformResourceName sipId = sipEntity.getSipIdUrn();
        UniformResourceName aipId = UniformResourceName.fromString(sipEntity.getSipIdUrn().toString());
        aipId.setIdentifier(OAISIdentifier.AIP);
        String fileName = UUID.randomUUID().toString();
        String storedUrl = "storage://in/the/place/" + fileToStoreChecksum;
        AIP aip = AIP.build(EntityType.DATA, aipId, Optional.of(sipId), providerId);
        aip.withDataObject(DataType.RAWDATA, Paths.get("file:///somewhere/", fileName), "MD5", fileToStoreChecksum);
        aip.withSyntax(mimeType);
        aip.registerContentInformation();
        AIPEntity aipEntity = AIPEntity.build(sipEntity, metaStorage ? AIPState.STORED : AIPState.GENERATED, aip);
        aipEntity.setChecksum(UUID.randomUUID().toString());
        aipEntity = aipRepo.save(aipEntity);
        Set<String> owners = Sets.newHashSet(aipId.toString());

        // Generated associated storage response
        String groupId = UUID.randomUUID().toString();
        Collection<RequestResultInfoDTO> results = Sets.newHashSet();
        if (metaStorage) {
            results.add(RequestResultInfoDTO
                    .build(groupId, aipEntity.getChecksum(), storage, storePath, owners, FileReferenceDTO
                            .build(OffsetDateTime.now(),
                                   FileReferenceMetaInfoDTO.build(aipEntity.getChecksum(), "MD5", fileName, 10L, null,
                                                                  null, MediaType.APPLICATION_JSON_UTF8, null),
                                   FileLocationDTO.build(storage, storedUrl), owners),
                           null));
            AIPStoreMetaDataRequest request = AIPStoreMetaDataRequest
                    .build(aipEntity, Sets.newHashSet(StoreLocation.build(storage, storePath)), true, true);
            request.setRemoteStepGroupIds(Lists.newArrayList(groupId));
            storeMetaReqRepo.save(request);

        } else {
            results.add(RequestResultInfoDTO
                    .build(groupId, fileToStoreChecksum, storage, storePath, owners, FileReferenceDTO
                            .build(OffsetDateTime.now(),
                                   FileReferenceMetaInfoDTO.build(fileToStoreChecksum, "MD5", fileName, 10L, null, null,
                                                                  MediaType.APPLICATION_JSON_UTF8, null),
                                   FileLocationDTO.build(storage, storedUrl), owners),
                           null));
            IngestRequest request = IngestRequest.build(IngestMetadata
                    .build(sessionOwner, session, "ingestChain", Sets.newHashSet(), StorageMetadata.build(storage)),
                                                        InternalRequestState.RUNNING, IngestRequestStep.LOCAL_INIT,
                                                        sip);
            request.setStep(IngestRequestStep.REMOTE_STORAGE_REQUESTED, 1000);
            // Create associated IngestRequest
            request.setRemoteStepGroupIds(Lists.newArrayList(groupId));
            request.setAips(Lists.newArrayList(aipEntity));
            ingestReqRepo.save(request);
        }

        return RequestInfo.build(groupId, results, Sets.newHashSet());
    }

    @Test
    public void testStorageResponsesHandler() throws ModuleException {
        Set<RequestInfo> responses = initAips(100, false);
        long start = System.currentTimeMillis();
        storageResponseFlowHandler.onStoreSuccess(responses);
        System.out.printf("Duration : %d ms", System.currentTimeMillis() - start);
        // Check results
        Assert.assertEquals(0,
                            requestService
                                    .findRequestDtos(SearchRequestsParameters.build().withSessionOwner("sessionOwner")
                                    .withRequestType(RequestTypeEnum.INGEST), PageRequest.of(0, 10))
                                    .getTotalElements());
        aipRepo.findAll().forEach(a -> {
            Assert.assertEquals(AIPState.STORED, a.getState());
        });
    }

    @Test
    public void testMetaStorageResponsesHandler() throws ModuleException {
        // NOTE : up this number for performances tests.
        int nbAipsToGenerated = 300;
        Set<RequestInfo> responses = initAips(nbAipsToGenerated, true);

        Iterator<RequestInfo> it = responses.iterator();

        int remaining = responses.size();
        do {
            long start = System.currentTimeMillis();
            Set<RequestInfo> subList = Sets.newHashSet();
            for (int i = 0; (i < 100) && it.hasNext(); i++) {
                subList.add(it.next());
                remaining--;
            }
            storageResponseFlowHandler.onStoreSuccess(subList);
            System.out.printf("Duration : %d ms \n", System.currentTimeMillis() - start);
            // Check results
            Assert.assertEquals(remaining,
                                requestService
                                        .findRequestDtos(SearchRequestsParameters.build().withSessionOwner("sessionOwner")
                                        .withRequestType(RequestTypeEnum.STORE_METADATA), PageRequest.of(0, 10))
                                        .getTotalElements());
            aipRepo.findAll().forEach(a -> {
                Assert.assertEquals(AIPState.STORED, a.getState());
            });
        } while (it.hasNext());

    }

}
