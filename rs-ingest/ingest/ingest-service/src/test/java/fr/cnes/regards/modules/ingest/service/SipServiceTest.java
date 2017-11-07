/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPEntityBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * @author Sébastien Binda
 */
@ActiveProfiles("testAmqp")
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
public class SipServiceTest extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private Gson gson;

    @Autowired
    private IAipClient aipClient;

    private SIPEntity sipWithManyAIPs = null;

    private SIPEntity sipWithOneAIP = null;

    private final Set<SIPEntity> sipWithManyVersions = Sets.newHashSet();

    private final List<AIP> simulatedStorageAips = new ArrayList<>();

    private static SipEventTestHandler handler = new SipEventTestHandler();

    @Before
    public void init() throws NoSuchAlgorithmException, IOException, InterruptedException {
        handler.clearEvents();
        subscriber.subscribeTo(SIPEvent.class, handler);
        aipRepository.deleteAll();
        sipRepository.deleteAll();
        // Initiate first sip session
        sipWithManyAIPs = sipRepository
                .save(createSIP("SIP_SERVICE_TEST_001", "SESSION_001", "PROCESSING_001", "OWNER_001", 1));
        sipWithOneAIP = sipRepository
                .save(createSIP("SIP_SERVICE_TEST_002", "SESSION_001", "PROCESSING_001", "OWNER_001", 1));
        sipWithManyVersions.add(sipRepository
                .save(createSIP("SIP_SERVICE_TEST_003", "SESSION_001", "PROCESSING_001", "OWNER_001", 1)));

        // Initiate third session with a new SIPs
        sipRepository.save(createSIP("SIP_SERVICE_TEST_004", "SESSION_003", "PROCESSING_002", "OWNER_002", 1));
        sipRepository.save(createSIP("SIP_SERVICE_TEST_005", "SESSION_003", "PROCESSING_002", "OWNER_002", 1));

        // Initiate sip without session
        SIPEntity errorSip = createSIP("SIP_SERVICE_TEST_006", null, "PROCESSING_003", "OWNER_001", 1);
        errorSip.setState(SIPState.AIP_GEN_ERROR);
        sipRepository.save(errorSip);

        // Initiate second sip session with sip new version
        sipWithManyVersions.add(sipRepository
                .save(createSIP("SIP_SERVICE_TEST_003", "SESSION_002", "PROCESSING_001", "OWNER_001", 2)));

        AIP aip = new AIP();
        aip.setState(AIPState.STORED);
        aip.setId(UniformResourceName.fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        aip.setSipId(sipWithManyAIPs.getIpId());
        simulatedStorageAips.add(aip);
        AIP aip2 = new AIP();
        aip2.setState(AIPState.STORED);
        aip2.setId(UniformResourceName.fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        aip2.setSipId(sipWithManyAIPs.getIpId());
        simulatedStorageAips.add(aip2);
        AIP aip3 = new AIP();
        aip3.setState(AIPState.STORED);
        aip3.setId(UniformResourceName.fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        aip3.setSipId(sipWithOneAIP.getIpId());
        simulatedStorageAips.add(aip3);
        for (SIPEntity entity : sipWithManyVersions) {
            AIP a = new AIP();
            a.setState(AIPState.STORED);
            a.setId(UniformResourceName
                    .fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V" + entity.getVersion()));
            a.setSipId(entity.getIpId());
            simulatedStorageAips.add(a);
        }
        // Simulate AIPClient to return AIPs associated to SIP
        Mockito.when(aipClient.retrieveAIPs(Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(),
                                            Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(invocation -> simulateRetrieveAIPResponseFromStorage((String) invocation
                        .getArguments()[0]));
    }

    @Requirement("REGARDS_DSL_ING_PRO_535")
    @Requirement("REGARDS_DSL_ING_PRO_610")
    @Requirement("REGARDS_DSL_ING_PRO_620")
    @Requirement("REGARDS_DSL_ING_PRO_630")
    @Requirement("REGARDS_DSL_ING_PRO_650")
    @Requirement("REGARDS_DSL_ING_PRO_660")
    @Purpose("Manage SIP deletion by ipId when SIP is associated to multiple AIPs")
    @Test
    public void deleteByIpIdMultiplesAIPs() throws InterruptedException {
        try {
            // 1. Run sip deletion by ipId
            sipService.deleteSIPEntity(sipWithManyAIPs.getIpId());
            // 1.1 Check call to the archival storage for deletion of associated AIPs.
            Mockito.verify(aipClient, Mockito.times(1)).deleteAipFromSips(Sets.newHashSet(sipWithManyAIPs.getIpId()));
            // 2. Simulate that one of the AIPs to delete has been successfully deleted by the archival storage microservice
            simulateAipDeletionFromStorage(getSipSimulatedAIPs(sipWithManyAIPs.getIpId()).get(0).getId());
            // 2.1 SIP should be in INCOMPLETE state as there is another AIP to delete
            Assert.assertTrue("SIP should be in INCOMPLETE state",
                              SIPState.INCOMPLETE.equals(sipRepository.findOne(sipWithManyAIPs.getId()).getState()));
            // 2.2 A SIPevent associated should have been sent
            Assert.assertTrue("A SIPEvent should had been sent with incomplete state for SIP",
                              handler.getReceivedEvents().stream()
                                      .anyMatch(e -> e.getIpId().equals(sipWithManyAIPs.getIpId())
                                              && e.getState().equals(SIPState.INCOMPLETE)));
            // 3 . Simulate the other AIP deleted by the archival storage microservice
            simulateAipDeletionFromStorage(getSipSimulatedAIPs(sipWithManyAIPs.getIpId()).get(1).getId());
            // 3.1 All AIP has been deleted, SIP should be in DELETED STATE
            SIPEntity deletedSip = sipRepository.findOne(sipWithManyAIPs.getId());
            Assert.assertTrue("SIP should be in DELETED state", SIPState.DELETED.equals(deletedSip.getState()));
            // 3.2 A SIPevent associated should have been sent
            Assert.assertTrue("A SIPEvent should had been sent with delete SIP IpId",
                              handler.getReceivedEvents().stream()
                                      .anyMatch(e -> e.getIpId().equals(sipWithManyAIPs.getIpId())
                                              && e.getState().equals(SIPState.DELETED)));
            // 3.2 SIP Last update date should be set to deletion date
            Assert.assertNotNull("Last update date should be set to deletion date for the deleted SIP",
                                 deletedSip.getLastUpdateDate());
            Assert.assertTrue("Last update date should be after ingestDate",
                              deletedSip.getIngestDate().compareTo(deletedSip.getLastUpdateDate()) < 0);
        } catch (ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Purpose("Manage SIP deletion by ipId when SIP is associated to only one AIP")
    @Test
    public void deleteByIpId() throws InterruptedException {
        try {
            // 1. Run sip deletion by ipId
            sipService.deleteSIPEntity(sipWithOneAIP.getIpId());
            // 1.1 Check call to the archival storage for deletion of associated AIPs.
            Mockito.verify(aipClient, Mockito.times(1)).deleteAipFromSips(Sets.newHashSet(sipWithOneAIP.getIpId()));
            // 2. Simulate that one of the AIPs to delete has been successfully deleted by the archival storage microservice
            simulateAipDeletionFromStorage(getSipSimulatedAIPs(sipWithOneAIP.getIpId()).get(0).getId());
            // 2.1 All AIP has been deleted, SIP should be in DELETED STATE
            SIPEntity deletedSip = sipRepository.findOne(sipWithOneAIP.getId());
            Assert.assertTrue("SIP should be in DELETED state", SIPState.DELETED.equals(deletedSip.getState()));
            // 2.1 A SIPevent associated should have been sent
            Assert.assertTrue("A SIPEvent should had been sent with delete SIP IpId",
                              handler.getReceivedEvents().stream()
                                      .anyMatch(e -> e.getIpId().equals(sipWithOneAIP.getIpId())
                                              && e.getState().equals(SIPState.DELETED)));
            // 2.2 SIP Last update date should be set to deletion date
            Assert.assertNotNull("Last update date should be set to deletion date for the deleted SIP",
                                 deletedSip.getLastUpdateDate());
            Assert.assertTrue("Last update date should be after ingestDate",
                              deletedSip.getIngestDate().compareTo(deletedSip.getLastUpdateDate()) < 0);
        } catch (ModuleException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Requirement("REGARDS_DSL_ING_PRO_535")
    @Requirement("REGARDS_DSL_ING_PRO_640")
    @Requirement("REGARDS_DSL_ING_PRO_660")
    @Purpose("Manage SIP deletion by sipId")
    @Test
    public void deleteBySipId() {
        try {
            // 1. Run sip deletion by sipId
            sipService.deleteSIPEntities(sipWithManyVersions.stream().findFirst().get().getSipId());
            // 1.1 Check call to the archival storage for deletion of AIPs of each SIP with the same sipId.
            Assert.assertTrue("For this test, the same SIP_ID should be associated to multiple SIPs",
                              sipWithManyVersions.size() > 1);
            for (SIPEntity entity : sipWithManyVersions) {
                Mockito.verify(aipClient, Mockito.times(1)).deleteAipFromSips(Sets.newHashSet(entity.getIpId()));
                // 2. Simulate that one of the AIPs to delete has been successfully deleted by the archival storage microservice
                simulateAipDeletionFromStorage(getSipSimulatedAIPs(entity.getIpId()).get(0).getId());
                // 2.1 All AIP has been deleted, SIP should be in DELETED STATE
                SIPEntity deletedSip = sipRepository.findOne(entity.getId());
                Assert.assertTrue("SIP should be in DELETED state", SIPState.DELETED.equals(deletedSip.getState()));
                // 2.1 A SIPevent associated should have been sent
                Assert.assertTrue("A SIPEvent should had been sent with delete SIP IpId", handler.getReceivedEvents()
                        .stream()
                        .anyMatch(e -> e.getIpId().equals(entity.getIpId()) && e.getState().equals(SIPState.DELETED)));
                // 2.2 SIP Last update date should be set to deletion date
                Assert.assertNotNull("Last update date should be set to deletion date for the deleted SIP",
                                     deletedSip.getLastUpdateDate());
                Assert.assertTrue("Last update date should be after ingestDate",
                                  deletedSip.getIngestDate().compareTo(deletedSip.getLastUpdateDate()) < 0);
            }

        } catch (ModuleException | InterruptedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Requirement("REGARDS_DSL_ING_PRO_520")
    @Purpose("Search for SIP by state")
    @Test
    public void searchSip() {
        // Check search by state
        Page<SIPEntity> results = sipService.getSIPEntities(null, null, null, null, SIPState.AIP_GEN_ERROR,
                                                            new PageRequest(0, 100));
        Assert.assertTrue("There should be only one AIP with AIP_GEN_ERROR state", results.getTotalElements() == 1);
    }

    @Requirement("REGARDS_DSL_ING_PRO_550")
    @Purpose("Manage indexed SIP")
    @Test
    public void indexSip() {
        // TODO : Check that if all AIPs are indexed then the SIP is in INDEXED state
    }

    @Requirement("REGARDS_DSL_ING_PRO_710")
    @Requirement("REGARDS_DSL_ING_PRO_720")
    @Requirement("REGARDS_DSL_ING_PRO_740")
    @Requirement("REGARDS_DSL_ING_PRO_750")
    @Requirement("REGARDS_DSL_ING_PRO_760")
    @Purpose("Manage sessions informations")
    @Test
    public void checkSessions() {
        // TODO : Check session id
        // TODO : Check session progress
        // TODO : Check session state ("STORED" if all SIP are in "STORED" state) ("INDEXED" if all SIP are "INDEXED")
        // TODO : Check all associated SIP state.
    }

    @Requirement("REGARDS_DSL_ING_PRO_830")
    @Requirement("REGARDS_DSL_ING_PRO_810")
    @Purpose("Manage session deletion")
    @Test
    public void deleteSession() {
        // TODO : Delete by id
        // TODO : Check assoatec SIP and AIP deletion
    }

    /**
     * Create a SIP for test initialization
     * @param sipId
     * @param sessionId
     * @param processing
     * @param owner
     * @param version
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    private SIPEntity createSIP(String sipId, String sessionId, String processing, String owner, Integer version)
            throws NoSuchAlgorithmException, IOException {
        SIPBuilder b = new SIPBuilder(sipId);
        InformationPackagePropertiesBuilder ippb = new InformationPackagePropertiesBuilder();
        ippb.addDescriptiveInformation("version", version.toString());
        SIP sip = b.build(ippb.build());
        SIPEntity sipEntity = SIPEntityBuilder.build(DEFAULT_TENANT, sessionId, sip, processing, owner, version,
                                                     SIPState.STORED, EntityType.DATA);
        sipEntity.setChecksum(SIPEntityBuilder.calculateChecksum(gson, sip, IngestService.MD5_ALGORITHM));
        return sipEntity;
    }

    /**
     * Simulate response from {@link IAipClient#retrieveAIPs(String, AIPState, java.time.OffsetDateTime, java.time.OffsetDateTime, int, int)}
     * @param sipId
     * @return
     */
    private ResponseEntity<PagedResources<Resource<AIP>>> simulateRetrieveAIPResponseFromStorage(String sipId) {
        Set<Resource<AIP>> resources = simulatedStorageAips.stream().filter(a -> a.getSipId().equals(sipId))
                .map(a -> new Resource<AIP>(a)).collect(Collectors.toSet());
        PagedResources<Resource<AIP>> pagedRes = new PagedResources<Resource<AIP>>(resources,
                new PageMetadata(resources.size(), resources.size(), resources.size()));
        return new ResponseEntity<>(pagedRes, HttpStatus.OK);

    }

    /**
     * Simulate actions done by microservice archival storage for an AIP deletion :
     * <ul>
     * <li>Set AIP to state DELETED</li>
     * <li>Send an AIPEvent throught AMQP</li>
     * </ul>
     * @param aipIpId
     * @throws InterruptedException
     */
    private void simulateAipDeletionFromStorage(UniformResourceName aipIpId) throws InterruptedException {
        Optional<AIP> oAip = simulatedStorageAips.stream().filter(a -> a.getId().equals(aipIpId)).findFirst();
        if (oAip.isPresent()) {
            AIP aipToDelete = oAip.get();
            aipToDelete.setState(AIPState.DELETED);
            publisher.publish(new AIPEvent(aipToDelete));
            Thread.sleep(1000);
        }
    }

    /**
     * Get all simulated archival storag AIPs intialized in the test for the given SIP.
     * @param sipId
     * @return
     */
    private List<AIP> getSipSimulatedAIPs(String sipId) {
        return simulatedStorageAips.stream().filter(a -> a.getSipId().equals(sipId)).collect(Collectors.toList());
    }

}
