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
package fr.cnes.regards.modules.ingest.service.flow;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.filecatalog.client.RequestInfo;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequestStep;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeConstant;
import fr.cnes.regards.modules.ingest.dto.request.RequestTypeEnum;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import fr.cnes.regards.modules.ingest.service.IngestMultitenantServiceIT;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * @author sbinda
 */
@TestPropertySource(properties = { "spring.jpa.show-sql=true",
                                   "spring.jpa.properties.hibernate.default_schema=ingest_store_flow_request_it" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles({ "noscheduler" })
public class StorageResponseFlowRequestIT extends IngestMultitenantServiceIT {

    @Autowired
    private StorageResponseFlowHandler storageResponseFlowHandler;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IIngestRequestRepository ingestReqRepo;

    @Autowired
    private ISIPRepository sipRepo;

    @Autowired
    private IAIPRepository aipRepo;

    private Set<RequestInfo> initAips(int nbAips) {
        Set<RequestInfo> rq = Sets.newHashSet();
        // Init 1000 sip/aip waiting for storage responses
        for (int i = 0; i < nbAips; i++) {
            rq.add(initAip(UUID.randomUUID().toString(), "aip_number_" + i));
        }
        return rq;
    }

    private RequestInfo initAip(String fileToStoreChecksum, String providerId) {
        String sessionOwner = "sessionOwner";
        String session = "session";
        String storage = "storage";
        String storePath = null;
        MimeType mimeType = MediaType.APPLICATION_JSON;
        SIPDto sip = SIPDto.build(EntityType.DATA, providerId);
        SIPEntity sipEntity = SIPEntity.build(getDefaultTenant(),
                                              IngestMetadata.build(sessionOwner,
                                                                   session,
                                                                   null,
                                                                   "ingestChain",
                                                                   Sets.newHashSet(),
                                                                   StorageMetadata.build(storage)),
                                              sip,
                                              1,
                                              SIPState.INGESTED);
        sipEntity.setChecksum(UUID.randomUUID().toString());
        sipEntity.setLastUpdate(OffsetDateTime.now());
        sipEntity = sipRepo.save(sipEntity);
        OaisUniformResourceName sipId = sipEntity.getSipIdUrn();
        OaisUniformResourceName aipId = OaisUniformResourceName.fromString(sipEntity.getSipIdUrn().toString());
        aipId.setIdentifier(OAISIdentifier.AIP);
        String fileName = UUID.randomUUID().toString();
        String storedUrl = "storage://in/the/place/" + fileToStoreChecksum;
        AIPDto aip = AIPDto.build(EntityType.DATA, aipId, Optional.of(sipId), providerId, sipEntity.getVersion());
        aip.withDataObject(DataType.RAWDATA, Paths.get("file:///somewhere/", fileName), "MD5", fileToStoreChecksum);
        aip.withSyntax(mimeType);
        aip.registerContentInformation();
        AIPEntity aipEntity = AIPEntity.build(sipEntity, AIPState.GENERATED, aip);
        aipEntity = aipRepo.save(aipEntity);
        Set<String> owners = Sets.newHashSet(aipId.toString());

        // Generated associated storage response
        String groupId = UUID.randomUUID().toString();
        Collection<RequestResultInfoDto> results = Sets.newHashSet();
        results.add(RequestResultInfoDto.build(groupId,
                                               fileToStoreChecksum,
                                               storage,
                                               storePath,
                                               owners,
                                               new FileReferenceDto(OffsetDateTime.now(),
                                                                    new FileReferenceMetaInfoDto(fileToStoreChecksum,
                                                                                                 "MD5",
                                                                                                 fileName,
                                                                                                 10L,
                                                                                                 null,
                                                                                                 null,
                                                                                                 MediaType.APPLICATION_JSON.toString(),
                                                                                                 null),
                                                                    new FileLocationDto(storage, storedUrl),
                                                                    owners),
                                               null));
        IngestRequest request = IngestRequest.build(null,
                                                    IngestMetadata.build(sessionOwner,
                                                                         session,
                                                                         null,
                                                                         "ingestChain",
                                                                         Sets.newHashSet(),
                                                                         StorageMetadata.build(storage)),
                                                    InternalRequestState.RUNNING,
                                                    IngestRequestStep.LOCAL_INIT,
                                                    sip);
        request.setStep(IngestRequestStep.REMOTE_STORAGE_REQUESTED, 1000);
        // Create associated IngestRequest
        request.setRemoteStepGroupIds(Lists.newArrayList(groupId));
        request.setAips(Lists.newArrayList(aipEntity));
        ingestReqRepo.save(request);

        return RequestInfo.build(groupId, results, Sets.newHashSet());
    }

    @Test
    public void testStorageResponsesHandler() {
        Set<RequestInfo> responses = initAips(100);
        long start = System.currentTimeMillis();
        storageResponseFlowHandler.onStoreSuccess(responses);
        System.out.printf("Duration : %d ms", System.currentTimeMillis() - start);
        // Check results
        // delete ingest requests if notification are active
        if (initDefaultNotificationSettings()) {
            mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
        }
        Assert.assertEquals(0,
                            requestService.findRequestDtos(new SearchRequestParameters().withSessionOwner("sessionOwner")
                                                                                        .withRequestIpTypesIncluded(Set.of(
                                                                                            RequestTypeEnum.INGEST)),
                                                           Pageable.ofSize(10)).getTotalElements());
        aipRepo.findAll().forEach(a -> {
            Assert.assertEquals(AIPState.STORED, a.getState());
        });
    }

    @Test
    public void testReferenceResponsesHandler() {
        Set<RequestInfo> responses = initAips(100);
        long start = System.currentTimeMillis();
        storageResponseFlowHandler.onReferenceSuccess(responses);
        System.out.printf("Duration : %d ms\n", System.currentTimeMillis() - start);
        // Check results
        // delete ingest requests if notification are active
        if (initDefaultNotificationSettings()) {
            mockNotificationSuccess(RequestTypeConstant.INGEST_VALUE);
        }
        Assert.assertEquals(0,
                            requestService.findRequestDtos(new SearchRequestParameters().withSessionOwner("sessionOwner")
                                                                                        .withRequestIpTypesIncluded(Set.of(
                                                                                            RequestTypeEnum.INGEST)),
                                                           Pageable.ofSize(10)).getTotalElements());
        aipRepo.findAll().forEach(a -> {
            Assert.assertEquals(AIPState.STORED, a.getState());
        });
    }

}
