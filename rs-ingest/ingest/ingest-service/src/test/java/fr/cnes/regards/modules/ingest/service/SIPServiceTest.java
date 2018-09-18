/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.PagedResources.PageMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.ingest.dao.ISIPSessionRepository;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.event.SIPEvent;
import fr.cnes.regards.modules.ingest.service.store.IAIPService;
import fr.cnes.regards.modules.storage.client.IAipClient;
import fr.cnes.regards.modules.storage.client.IAipEntityClient;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.RejectedSip;
import fr.cnes.regards.modules.storage.domain.database.AIPEntity;
import fr.cnes.regards.modules.storage.domain.event.AIPEvent;

/**
 * @author Sébastien Binda
 */
public class SIPServiceTest extends AbstractSIPTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SIPServiceTest.class);

    private final static String COMPLEX_SESSION_ID = "SESSION_100";

    private static SIPEventTestHandler handler = new SIPEventTestHandler();

    private final Set<SIPEntity> sipWithManyVersions = Sets.newHashSet();

    private final List<AIP> simulatedStorageAips = new ArrayList<>();

    private final Set<SIPEntity> complexSessionSips = Sets.newHashSet();

    @Autowired
    private ISIPSessionRepository sipSessionRepository;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private ISubscriber subscriber;

    @Autowired
    private IPublisher publisher;

    @Autowired
    private IAipClient aipClient;

    @Autowired
    private IAipEntityClient aipEntityClient;

    @Autowired
    private IAIPService aipService;

    private SIPEntity sipWithManyAIPs = null;

    private SIPEntity sipWithOneAIP = null;

    @Override
    @SuppressWarnings("unchecked")
    public void doInit() throws NoSuchAlgorithmException, IOException, InterruptedException, ModuleException {
        handler.clearEvents();
        subscriber.subscribeTo(SIPEvent.class, handler);
        sipSessionRepository.deleteAll();
        Mockito.reset(aipClient);

        // Initiate first sip session
        sipWithManyAIPs = createSIP("SIP_SERVICE_TEST_001", "SESSION_001", "PROCESSING_001", "OWNER_001", 1);
        sipWithOneAIP = createSIP("SIP_SERVICE_TEST_002", "SESSION_001", "PROCESSING_001", "OWNER_001", 1);
        sipWithManyVersions.add(createSIP("SIP_SERVICE_TEST_003", "SESSION_001", "PROCESSING_001", "OWNER_001", 1));

        // Initiate third session with a new SIPs
        createSIP("SIP_SERVICE_TEST_004", "SESSION_003", "PROCESSING_002", "OWNER_002", 1);
        createSIP("SIP_SERVICE_TEST_005", "SESSION_003", "PROCESSING_002", "OWNER_002", 1);

        // Initiate sip without session
        createSIP("SIP_SERVICE_TEST_006", null, "PROCESSING_003", "OWNER_001", 1);

        // Initiate second sip session with sip new version
        sipWithManyVersions.add(createSIP("SIP_SERVICE_TEST_003", "SESSION_002", "PROCESSING_001", "OWNER_001", 2));

        // Initiate a complex session with 2 SIP in all states
        int id = 100;
        for (SIPState state : SIPState.values()) {
            complexSessionSips.add(createSIP("SIP_SERVICE_TEST_" + id,
                                             COMPLEX_SESSION_ID,
                                             "PROCESSING_001",
                                             "OWNER_003",
                                             1,
                                             state));
            id++;
            complexSessionSips.add(createSIP("SIP_SERVICE_TEST_" + id,
                                             COMPLEX_SESSION_ID,
                                             "PROCESSING_001",
                                             "OWNER_003",
                                             1,
                                             state));
            id++;
        }

        AIP aip = new AIP();
        aip.setState(AIPState.STORED);
        aip.setId(UniformResourceName.fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        aip.setSipId(sipWithManyAIPs.getSipIdUrn());
        simulatedStorageAips.add(aip);
        AIP aip2 = new AIP();
        aip2.setState(AIPState.STORED);
        aip2.setId(UniformResourceName.fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        aip2.setSipId(sipWithManyAIPs.getSipIdUrn());
        simulatedStorageAips.add(aip2);
        AIP aip3 = new AIP();
        aip3.setState(AIPState.STORED);
        aip3.setId(UniformResourceName.fromString("URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V1"));
        aip3.setSipId(sipWithOneAIP.getSipIdUrn());
        simulatedStorageAips.add(aip3);
        for (SIPEntity entity : sipWithManyVersions) {
            AIP a = new AIP();
            a.setState(AIPState.STORED);
            a.setId(UniformResourceName.fromString(
                    "URN:AIP:DATA:DEFAULT:" + UUID.randomUUID().toString() + ":V" + entity.getVersion()));
            a.setSipId(entity.getSipIdUrn());
            simulatedStorageAips.add(a);
        }

        // Simulate AIPClient to return empty rejected SIP from AIPs deletion
        Mockito.when(aipClient.deleteAipFromSips(Mockito.anySet()))
                .thenReturn(simulateDeleteSIPAIPsREsponseFromStorage());
    }

    @Requirement("REGARDS_DSL_ING_PRO_535")
    @Requirement("REGARDS_DSL_ING_PRO_610")
    @Requirement("REGARDS_DSL_ING_PRO_620")
    @Requirement("REGARDS_DSL_ING_PRO_630")
    @Requirement("REGARDS_DSL_ING_PRO_650")
    @Requirement("REGARDS_DSL_ING_PRO_660")
    @Purpose("Manage SIP deletion by ipId when SIP is associated to multiple AIPs")
    @Test
    public void deleteBySipIdMultiplesAIPs() throws InterruptedException {

        // Simulate AIPClient to return AIPs associated to SIP
        Mockito.when(aipEntityClient.retrieveAIPEntities(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(invocation -> simulateRetrieveAIPResponseFromStorage((String) invocation
                        .getArguments()[0]));

        try {
            // 1. Run sip deletion by ipId
            sipService.deleteSIPEntitiesBySipIds(Sets.newHashSet(sipWithManyAIPs.getSipIdUrn()));
            aipService.askForAipsDeletion();
            // 1.1 Check call to the archival storage for deletion of associated AIPs.
            Mockito.verify(aipClient, Mockito.times(1))
                    .deleteAipFromSips(Sets.union(Sets.newHashSet(sipWithManyAIPs.getSipId().toString()),
                                                  complexSessionSips.stream()
                                                          .filter(sip -> sip.getState() == SIPState.TO_BE_DELETED)
                                                          .map(SIPEntity::getSipId).collect(Collectors.toSet())));
            // 2. Simulate that one of the AIPs to delete has been successfully deleted by the archival storage
            // microservice
            simulateAipDeletionFromStorage(getSipSimulatedAIPs(sipWithManyAIPs.getSipId().toString()).get(0).getId());
            // 2.1 SIP should be in INCOMPLETE state as there is another AIP to delete
            Assert.assertTrue("SIP should be in INCOMPLETE state",
                              SIPState.INCOMPLETE.equals(sipRepository.findOne(sipWithManyAIPs.getId()).getState()));
            // 2.2 A SIPevent associated should have been sent
            Assert.assertTrue("A SIPEvent should had been sent with incomplete state for SIP",
                              handler.getReceivedEvents().stream()
                                      .anyMatch(e -> e.getSipId().equals(sipWithManyAIPs.getSipId()) && e.getState()
                                              .equals(SIPState.INCOMPLETE)));
            // 3 . Simulate the other AIP deleted by the archival storage microservice
            simulateAipDeletionFromStorage(getSipSimulatedAIPs(sipWithManyAIPs.getSipId().toString()).get(1).getId());
            // 3.1 All AIP has been deleted, SIP should be in DELETED STATE
            SIPEntity deletedSip = sipRepository.findOne(sipWithManyAIPs.getId());
            LOGGER.debug("Deleted SIP state : {}", deletedSip.getState());
            Assert.assertTrue("SIP should be in DELETED state", SIPState.DELETED.equals(deletedSip.getState()));
            // 3.2 A SIPevent associated should have been sent
            Assert.assertTrue("A SIPEvent should had been sent with delete SIP sipId",
                              handler.getReceivedEvents().stream()
                                      .anyMatch(e -> e.getSipId().equals(sipWithManyAIPs.getSipId().toString()) && e
                                              .getState().equals(SIPState.DELETED)));
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
    public void deleteBySipId() throws InterruptedException {

        // Simulate AIPClient to return AIPs associated to SIP
        Mockito.when(aipEntityClient.retrieveAIPEntities(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(invocation -> simulateRetrieveAIPResponseFromStorage((String) invocation
                        .getArguments()[0]));

        try {
            // 1. Run sip deletion by ipId
            sipService.deleteSIPEntitiesBySipIds(Sets.newHashSet(sipWithOneAIP.getSipIdUrn()));
            aipService.askForAipsDeletion();
            // 1.1 Check call to the archival storage for deletion of associated AIPs.
            Mockito.verify(aipClient, Mockito.times(1))
                    .deleteAipFromSips(Sets.union(Sets.newHashSet(sipWithOneAIP.getSipId().toString()),
                                                  complexSessionSips.stream()
                                                          .filter(sip -> sip.getState() == SIPState.TO_BE_DELETED)
                                                          .map(SIPEntity::getSipId).collect(Collectors.toSet())));
            // 2. Simulate that one of the AIPs to delete has been successfully deleted by the archival storage
            // microservice
            simulateAipDeletionFromStorage(getSipSimulatedAIPs(sipWithOneAIP.getSipId().toString()).get(0).getId());
            // 2.1 All AIP has been deleted, SIP should be in DELETED STATE
            SIPEntity deletedSip = sipRepository.findOne(sipWithOneAIP.getId());
            Assert.assertEquals(SIPState.DELETED, deletedSip.getState());
            // 2.1 A SIPevent associated should have been sent
            Assert.assertTrue("A SIPEvent should had been sent with delete SIP sipId",
                              handler.getReceivedEvents().stream()
                                      .anyMatch(e -> e.getSipId().equals(sipWithOneAIP.getSipId().toString()) && e
                                              .getState().equals(SIPState.DELETED)));
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
    @Purpose("Manage SIP deletion by provider id")
    @Test
    public void deleteByProviderId() {

        // Simulate AIPClient to return AIPs associated to SIP
        Mockito.when(aipEntityClient.retrieveAIPEntities(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt()))
                .thenAnswer(invocation -> simulateRetrieveAIPResponseFromStorage((String) invocation
                        .getArguments()[0]));

        try {
            // 1. Run sip deletion by provider id
            sipService.deleteSIPEntitiesForProviderId(sipWithManyVersions.stream().findFirst().get().getProviderId());
            aipService.askForAipsDeletion();
            // 1.1 Check call to the archival storage for deletion of AIPs of each SIP with the same provider id.
            Assert.assertTrue("For this test, the same SIP_ID should be associated to multiple SIPs",
                              sipWithManyVersions.size() > 1);
            Mockito.verify(aipClient, Mockito.times(1))
                    .deleteAipFromSips(Sets.union(sipWithManyVersions.stream().map(sip -> sip.getSipId().toString())
                                                          .collect(Collectors.toSet()),
                                                  complexSessionSips.stream()
                                                          .filter(sip -> sip.getState() == SIPState.TO_BE_DELETED)
                                                          .map(SIPEntity::getSipId).collect(Collectors.toSet())));
            for (SIPEntity sip : sipWithManyVersions) {
                // 2. Simulate that one of the AIPs to delete has been successfully deleted by the archival storage
                // microservice
                simulateAipDeletionFromStorage(getSipSimulatedAIPs(sip.getSipId()).get(0).getId());
                // 2.1 All AIP has been deleted, SIP should be in DELETED STATE
                SIPEntity deletedSip = sipRepository.findOne(sip.getId());
                LOGGER.debug("Deleted SIP state : {}", deletedSip.getState());
                Assert.assertEquals("SIP should be in DELETED state", SIPState.DELETED, deletedSip.getState());
                // 2.1 A SIPevent associated should have been sent
                Assert.assertTrue("A SIPEvent should had been sent with delete SIP sipId",
                                  handler.getReceivedEvents().stream()
                                          .anyMatch(e -> e.getSipId().equals(sip.getSipId()) && e.getState()
                                                  .equals(SIPState.DELETED)));
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
        Page<SIPEntity> results = sipService.search(null,
                                                    null,
                                                    null,
                                                    null,
                                                    Lists.newArrayList(SIPState.AIP_GEN_ERROR),
                                                    null,
                                                    new PageRequest(0, 100));
        Assert.assertTrue("There should be only two AIPs with AIP_GEN_ERROR state", results.getTotalElements() == 2);
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
        // Check retrieve sessions
        Page<SIPSession> result = sipSessionService.search(null, null, null, new PageRequest(0, 100));
        Assert.assertTrue(result.getTotalElements() == 5);
        SIPSession prevSession = null;
        // Check order by last activation date.
        for (SIPSession session : result.getContent()) {
            if (prevSession != null) {
                Assert.assertTrue(session.getLastActivationDate().compareTo(prevSession.getLastActivationDate()) <= 0);
            }
            prevSession = session;
        }
        // Check session progress for complex simulated session
        SIPSession session = result.getContent().stream().filter(s -> COMPLEX_SESSION_ID.equals(s.getId())).findFirst()
                .get();
        Assert.assertNotNull(session);
        // There is 2 SIPS for each state in this simulated session
        Assert.assertTrue(session.getSipsCount() == (SIPState.values().length * 2));
        Assert.assertTrue(session.getIndexedSipsCount() == 2);
        Assert.assertTrue(session.getStoredSipsCount() == 6);
        Assert.assertTrue(session.getGeneratedSipsCount() == 12);
        Assert.assertTrue(session.getErrorSipsCount() == 10);
        Assert.assertTrue(session.getDeletedSipsCount() == 2);

    }

    @SuppressWarnings("unchecked")
    @Requirement("REGARDS_DSL_ING_PRO_830")
    @Requirement("REGARDS_DSL_ING_PRO_810")
    @Purpose("Manage session deletion")
    @Test
    public void deleteSession() throws ModuleException {
        // Delete by session id
        Collection<RejectedSip> rejectedSips = sipService.deleteSIPEntitiesForSessionId(COMPLEX_SESSION_ID);
        aipService.askForAipsDeletion();
        // 2 SIP per state in COMPLEX_SESSION_ID.
        // Undeletable are QUEUED, VALID, TO_BE_DELETED, DELETED
        Assert.assertEquals(8, rejectedSips.size());
        // Check call to storage client for deletion
        @SuppressWarnings("rawtypes") ArgumentCaptor<Set> argument = ArgumentCaptor.forClass(Set.class);
        Mockito.verify(aipClient, Mockito.times(1)).deleteAipFromSips(argument.capture());
        // Valid SIP for deletion are other states (CREATED, AIP_CREATED, INVALID, AIP_GEN_ERROR, REJECTED, STORED,
        // STORE_ERROR, INCOMPLETE, INDEXED, INDEX_ERROR, TO_BE_DELETED)
        Assert.assertEquals(12, argument.getValue().size());
        // Check that not stored SIP are already in DELETED state
        // Not stored state are CREATED, AIP_CREATED, INVALID, AIP_GEN_ERROR, REJECTED, DELETED
        Page<SIPEntity> results = sipService.search(null,
                                                    COMPLEX_SESSION_ID,
                                                    null,
                                                    null,
                                                    Lists.newArrayList(SIPState.DELETED),
                                                    null,
                                                    new PageRequest(0, 100));
        Assert.assertEquals(24, results.getTotalElements());

    }

    /**
     * Simulate response from
     * {@link IAipClient#retrieveAIPs(AIPState, java.time.OffsetDateTime, java.time.OffsetDateTime, int, int)}
     */
    private ResponseEntity<PagedResources<Resource<AIPEntity>>> simulateRetrieveAIPResponseFromStorage(String sipId) {
        Set<Resource<AIPEntity>> resources = simulatedStorageAips.stream().filter(a -> a.getSipId().get().equals(sipId))
                .map(a -> {
                    AIPEntity entity = new AIPEntity();
                    entity.setAip(a);
                    entity.setState(a.getState());
                    entity.setAipId(a.getId().toString());
                    entity.setSipId(a.getSipId().orElse(null));
                    return new Resource<AIPEntity>(entity);
                }).collect(Collectors.toSet());
        PagedResources<Resource<AIPEntity>> pagedRes = new PagedResources<Resource<AIPEntity>>(resources,
                                                                                               new PageMetadata(
                                                                                                       resources.size(),
                                                                                                       resources.size(),
                                                                                                       resources
                                                                                                               .size()));
        return new ResponseEntity<>(pagedRes, HttpStatus.OK);
    }

    /**
     * Simulate response from {@link IAipClient#deleteAipFromSips(Set)}
     * @return
     */
    private ResponseEntity<List<RejectedSip>> simulateDeleteSIPAIPsREsponseFromStorage() {
        List<RejectedSip> rejectedSips = Lists.newArrayList();
        return new ResponseEntity<>(rejectedSips, HttpStatus.OK);
    }

    /**
     * Simulate actions done by microservice archival storage for an AIP deletion :
     * <ul>
     * <li>Set AIP to state DELETED</li>
     * <li>Send an AIPEvent throught AMQP</li>
     * </ul>
     * @param aipId
     * @throws InterruptedException
     */
    private void simulateAipDeletionFromStorage(UniformResourceName aipId) throws InterruptedException {
        Optional<AIP> oAip = simulatedStorageAips.stream().filter(a -> a.getId().equals(aipId)).findFirst();
        if (oAip.isPresent()) {
            AIP aipToDelete = oAip.get();
            aipToDelete.setState(AIPState.DELETED);
            publisher.publish(new AIPEvent(aipToDelete));
            Thread.sleep(3000);
        }
    }

    /**
     * Get all simulated archival storag AIPs intialized in the test for the given SIP.
     */
    private List<AIP> getSipSimulatedAIPs(String sipId) {
        return simulatedStorageAips.stream().filter(a -> a.getSipId().get().equals(sipId)).collect(Collectors.toList());
    }

}
