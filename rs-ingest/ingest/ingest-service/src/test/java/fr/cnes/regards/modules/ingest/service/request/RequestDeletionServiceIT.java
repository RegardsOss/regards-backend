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
package fr.cnes.regards.modules.ingest.service.request;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.oais.dto.ContentInformationDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectLocationDto;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
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
import fr.cnes.regards.modules.ingest.service.aip.IAIPDeleteService;
import fr.cnes.regards.modules.ingest.service.aip.IAIPService;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Stephane Cortine
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=request_deletion_it" },
                    locations = { "classpath:application-test.properties" })
public class RequestDeletionServiceIT extends AbstractMultitenantServiceIT {

    @Autowired
    private RequestDeletionService requestDeletionService;

    @Autowired
    private IAIPDeleteService aipDeleteService;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IRequestService requestService;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    //@SpyBean
    private IPublisher publisher;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    private IAbstractRequestRepository abstractRequestRepository;

    @Before
    public void setup() {
        abstractRequestRepository.deleteAll();
        aipRepository.deleteAllInBatch();
        sipRepository.deleteAllInBatch();
    }

    @Test
    public void test_delete_requests() {
        //Given
        IngestRequest ingestRequest = createIngestRequest("SIP_001");
        IngestRequest ingestRequest2 = createIngestRequest("SIP_002");

        //When
        requestDeletionService.deleteRequests(List.of(ingestRequest, ingestRequest2));

        //Then
        Assert.assertEquals(0, abstractRequestRepository.findAll().size());
        Assert.assertEquals(0, aipRepository.findAll().size());
        Assert.assertEquals(0, sipRepository.findAll().size());
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    public IngestRequest createIngestRequest(String sipId) {

        SIPEntity sipEntity = new SIPEntity();
        SIPDto sipDto = SIPDto.build(EntityType.DATA, sipId)
                              .withDescriptiveInformation("version", "2")

                              .withDataObjectReference(DataType.AIP, "filename1", "url", "storage")
                              .withContextInformation("key1", "value1");

        ContentInformationDto contentInformationDto = new ContentInformationDto();
        OAISDataObjectDto oAISDataObjectDto = new OAISDataObjectDto();
        oAISDataObjectDto.setAlgorithm("MD5");
        oAISDataObjectDto.setFilename("filename.dat");
        oAISDataObjectDto.setChecksum("123a123" + sipId);
        oAISDataObjectDto.setLocations(Set.of(OAISDataObjectLocationDto.build("file:///input/validation/filename.dat")));
        oAISDataObjectDto.setRegardsDataType(DataType.RAWDATA);

        contentInformationDto.setDataObject(oAISDataObjectDto);
        sipDto.getProperties().setContentInformations(List.of(contentInformationDto));

        sipEntity.setSip(sipDto);
        sipEntity.setSipId(OaisUniformResourceName.fromString("URN:SIP:COLLECTION:DEFAULT:"
                                                              + UUID.randomUUID()
                                                              + ":V1"));
        sipEntity.setProviderId(sipId);
        sipEntity.setCreationDate(OffsetDateTime.now().minusHours(6));
        sipEntity.setLastUpdate(OffsetDateTime.now().minusHours(6));
        sipEntity.setSessionOwner("SESSION_OWNER");
        sipEntity.setSession("SESSION");
        sipEntity.setCategories(org.assertj.core.util.Sets.newLinkedHashSet("CATEGORIES"));
        sipEntity.setState(SIPState.INGESTED);
        sipEntity.setVersion(1);
        sipEntity.setChecksum("123456789032" + sipId);

        sipEntity = sipRepository.save(sipEntity);

        AIPDto aipDto = AIPDto.build(sipEntity.getSip(),
                                     OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                             EntityType.DATA,
                                                                             "tenant",
                                                                             1),
                                     Optional.empty(),
                                     sipEntity.getProviderId(),
                                     sipEntity.getVersion());
        //aipDto.getProperties().s
        AIPEntity aipEntity = AIPEntity.build(sipEntity, AIPState.GENERATED, aipDto);

        aipEntity = aipRepository.save(aipEntity);

        IngestRequest ingestRequest = IngestRequest.build("REQUEST_ID_TEST",
                                                          IngestMetadata.build("SESSION_OWNER",
                                                                               "SESSION",
                                                                               OffsetDateTime.now(),
                                                                               "ingestChain",
                                                                               new HashSet<>(),
                                                                               StorageMetadata.build("RAS")),
                                                          InternalRequestState.ERROR,
                                                          IngestRequestStep.LOCAL_SCHEDULED,
                                                          aipEntity.getSip().getSip());
        // Set list of AIPs to ingest request
        ingestRequest.setAips(List.of(aipEntity));

        return abstractRequestRepository.save(ingestRequest);
    }

}
