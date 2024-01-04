package fr.cnes.regards.modules.ingest.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.request.ChooseVersioningRequestParameters;
import fr.cnes.regards.modules.ingest.service.job.ChooseVersioningJob;
import fr.cnes.regards.modules.ingest.service.request.IIngestRequestService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import fr.cnes.regards.modules.ingest.service.session.SessionNotifier;
import fr.cnes.regards.modules.storage.client.test.StorageClientMock;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import javax.persistence.criteria.Predicate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static fr.cnes.regards.modules.ingest.dao.AbstractRequestSpecifications.STATE_ATTRIBUTE;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=ingestversioning",
                                   "spring.jpa.show-sql=false",
                                   "regards.amqp.enabled=true",
                                   "spring.task.scheduling.pool.size=4",
                                   "regards.ingest.maxBulkSize=100",
                                   "eureka.client.enabled=false",
                                   "regards.aips.save-metadata.bulk.delay=100",
                                   "regards.ingest.aip.delete.bulk.delay=100" },
                    locations = { "classpath:application-test.properties" })
@ActiveProfiles(value = { "testAmqp", "StorageClientMock" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class VersioningModeIT extends IngestMultitenantServiceIT {

    public static final String SESSION_0 = OffsetDateTime.now().toString();

    private static final List<String> CATEGORIES_0 = Lists.newArrayList("CATEGORY");

    private static final List<String> TAG_0 = Lists.newArrayList("toto", "tata");

    private static final List<String> TAG_1 = Lists.newArrayList("toto", "tutu");

    private static final List<String> TAG_2 = Lists.newArrayList("toto", "tutu", "tata");

    private static final List<String> TAG_3 = Lists.newArrayList("toto", "tutu", "titi");

    private static final String STORAGE_0 = "fake";

    private static final String SESSION_OWNER_0 = "NASA";

    private static final String PROVIDER_ID = "provider 1";

    @Autowired
    private StorageClientMock storageClient;

    @SpyBean
    private SessionNotifier sessionNotifier;

    @Autowired
    private IIngestRequestService ingestRequestService;

    @Autowired
    private IRequestService requestService;

    @Override
    public void doInit() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
    }

    /**
     * Lets test that everything goes well with {@link VersioningMode#INC_VERSION}.
     * That means:
     * - submit 2 versions of the same SIP
     * - verify that the 2 versions are stored
     * - only the second one is latest
     * - verify that also applies to the 2 AIP generated
     */
    @Test
    public void testIncVersion() {
        storageClient.setBehavior(true, true);

        // lets submit the first SIP
        publishSIPEvent(create(PROVIDER_ID, TAG_0), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        ingestServiceTest.waitForAIP(1, 20000, AIPState.STORED, getDefaultTenant());
        // lets check that first SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());

        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));

        // lets submit the second SIP with different TAGS so it is accepted by system
        publishSIPEvent(create(PROVIDER_ID, TAG_1), STORAGE_0, SESSION_0, SESSION_OWNER_0, CATEGORIES_0);
        ingestServiceTest.waitForAIP(2, 20000, AIPState.STORED, getDefaultTenant());
        // lets check that second SIP version is the latest
        sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, sips.length);
        Assert.assertTrue(String.format(
            "This SIP should be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[1].getVersion(),
            PROVIDER_ID), sips[1].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[1].getState());
        Assert.assertFalse(String.format(
            "This SIP should not be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[0].getVersion(),
            PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIPs
        aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, aips.length);
        Assert.assertTrue(String.format(
            "This AIP should be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[1].getVersion(),
            PROVIDER_ID), aips[1].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[1].getState());
        Assert.assertFalse(String.format(
            "This AIP should not be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[0].getVersion(),
            PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));
    }

    /**
     * Lets test that everything goes well with {@link VersioningMode#REPLACE}.
     * That means:
     * - submit 2 versions of the same SIP
     * - verify that only the second version is stored
     * - only the second one is latest
     * - verify that also applies to the 2 AIP generated
     */
    @Test
    public void testReplace() {
        storageClient.setBehavior(true, true);

        // lets submit the first SIP
        publishSIPEvent(create(PROVIDER_ID, TAG_0),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.REPLACE);
        ingestServiceTest.waitForAIP(1, 20000, AIPState.STORED, getDefaultTenant());
        // lets check that first SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());

        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));

        // lets submit the second SIP with different TAGS so it is accepted by system
        publishSIPEvent(create(PROVIDER_ID, TAG_1),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.REPLACE);
        ingestServiceTest.waitForAIP(2, 20000, AIPState.STORED, getDefaultTenant());
        // once the 2 AIPs are stored, we ask for the deletion of the old one, so lets wait for this deletion
        ingestServiceTest.waitForAIP(1, 20_000, AIPState.DELETED, getDefaultTenant());
        // lets check that second SIP version is the latest
        sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, sips.length);
        Assert.assertTrue(String.format(
            "This SIP should be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[1].getVersion(),
            PROVIDER_ID), sips[1].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[1].getState());
        Assert.assertFalse(String.format(
            "This SIP should not be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[0].getVersion(),
            PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.DELETED),
                            SIPState.DELETED,
                            sips[0].getState());
        // lets check associated AIPs
        aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, aips.length);
        Assert.assertTrue(String.format(
            "This AIP should be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[1].getVersion(),
            PROVIDER_ID), aips[1].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[1].getState());
        Assert.assertFalse(String.format(
            "This AIP should not be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[0].getVersion(),
            PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.DELETED),
                            AIPState.DELETED,
                            aips[0].getState());

        // session monitoring
        // 1 - for new version
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));
        // 2 - for old version removal
        // there is the request from 2 IngestRequest, OAISDeletionCreatorRequest and OAISDeletionRequest that are deleted
        Mockito.verify(sessionNotifier, Mockito.times(4)).requestDeleted(Mockito.any(AbstractRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .productDeleted(Mockito.eq(SESSION_OWNER_0), Mockito.eq(SESSION_0), Mockito.anyCollection());
    }

    @Test
    public void testIncWithTwoVersionAtSameTime() {
        // this allows to have a first version than a second version replacing the first(which is now in state DELETED)
        testIncVersion();
        // lets submit the third and forth SIP with different TAGS so it is accepted by system
        publishSIPEvent(Lists.newArrayList(create(PROVIDER_ID, TAG_2), create(PROVIDER_ID, TAG_3)),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.INC_VERSION);
        ingestServiceTest.waitForAIP(4, 20_000, AIPState.STORED, getDefaultTenant());
    }

    @Test
    public void testReplaceWithTwoVersionAtSameTime() {
        // this allows to have a first version than a second version replacing the first(which is now in state DELETED)
        testReplace();
        // lets submit the third and forth SIP with different TAGS so it is accepted by system
        publishSIPEvent(Lists.newArrayList(create(PROVIDER_ID, TAG_2), create(PROVIDER_ID, TAG_3)),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.REPLACE);

        // once the 2 new AIPs are stored, we ask for the deletion of the old ones, so lets wait for this deletion
        // 3 = V1 which has already been deleted previously, V2 which is replaced by V3 and V2 which should be replaced by V4
        ingestServiceTest.waitForAIP(3, 40_000, AIPState.DELETED, getDefaultTenant());
        ingestServiceTest.waitForAIP(1, 1_000, AIPState.STORED, getDefaultTenant());
        // lets check that second SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only four SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 4, sips.length);
        Assert.assertTrue(String.format(
            "This SIP should be the latest as it is version %s out of 4 SIP for providerId \"%s\"",
            sips[3].getVersion(),
            PROVIDER_ID), sips[3].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[3].getState());
        Assert.assertFalse(String.format(
            "This SIP should not be the latest as it is version %s out of 4 SIP for providerId \"%s\"",
            sips[2].getVersion(),
            PROVIDER_ID), sips[2].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.DELETED),
                            SIPState.DELETED,
                            sips[2].getState());
        // lets check associated AIPs
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only 4 AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 4, aips.length);
        Assert.assertTrue(String.format(
            "This AIP should be the latest as it is version %s out of 4 AIP for providerId \"%s\"",
            aips[3].getVersion(),
            PROVIDER_ID), aips[3].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[3].getState());
        Assert.assertFalse(String.format(
            "This AIP should not be the latest as it is version %s out of 4 AIP for providerId \"%s\"",
            aips[2].getVersion(),
            PROVIDER_ID), aips[2].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.DELETED),
                            AIPState.DELETED,
                            aips[2].getState());

        // session monitoring
        // 1 - for new version
        Mockito.verify(sessionNotifier, Mockito.times(4))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(4))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(4))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(4))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(4))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));
        // 6 - for old version removal
        // there is the request from IngestRequests, OAISDeletionCreatorRequest and OAISDeletionRequest that are deleted
        Mockito.verify(sessionNotifier, Mockito.times(10)).requestDeleted(Mockito.any(AbstractRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .productDeleted(Mockito.eq(SESSION_OWNER_0), Mockito.eq(SESSION_0), Mockito.anyCollection());
    }

    /**
     * Lets test that everything goes well with {@link VersioningMode#IGNORE}.
     * That means:
     * - submit first version of a SIP
     * - verify that it is stored and latest
     * - submit a second version of a SIP
     * - verify that the request is IGNORED
     */
    @Test
    public void testIgnore() {
        storageClient.setBehavior(true, true);

        // lets submit the first SIP
        publishSIPEvent(create(PROVIDER_ID, TAG_0),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.IGNORE);
        ingestServiceTest.waitForAIP(1, 20000, AIPState.STORED, getDefaultTenant());
        // lets check that first SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());

        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));

        // lets submit the second SIP with different TAGS so it is accepted by system
        publishSIPEvent(create(PROVIDER_ID, TAG_1),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.IGNORE);
        ingestServiceTest.waitForIngestRequest(1, 20_000, InternalRequestState.IGNORED, getDefaultTenant());

        // Well nothing should have changed
        // lets check that first SIP version is the latest
        sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());
        // check that session has been notified
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1)).incrementProductIgnored(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        // check that request is in state IGNORE, there is only this test request in DB so if there is one IGNORED, it is ours
        Assert.assertEquals("There should be one request in IGNORED state",
                            1,
                            ingestRequestRepository.countByState(InternalRequestState.IGNORED));

    }

    /**
     * Lets test that everything goes well with {@link VersioningMode#MANUAL}.
     * That means:
     * - submit first version of a SIP
     * - verify that the request ends in state {@link InternalRequestState#WAITING_VERSIONING_MODE}
     * - no sip is created
     */
    @Test
    public void testManual() {
        storageClient.setBehavior(true, true);

        // lets submit the first SIP
        publishSIPEvent(create(PROVIDER_ID, TAG_0),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.MANUAL);
        ingestServiceTest.waitForAIP(1, 20000, AIPState.STORED, getDefaultTenant());
        // lets check that first SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());

        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));

        // lets submit the second SIP with different TAGS so it is accepted by system
        publishSIPEvent(create(PROVIDER_ID, TAG_1),
                        Lists.newArrayList(STORAGE_0),
                        SESSION_0,
                        SESSION_OWNER_0,
                        CATEGORIES_0,
                        Optional.empty(),
                        VersioningMode.MANUAL);
        ingestServiceTest.waitForIngestRequest(1,
                                               20_000,
                                               InternalRequestState.WAITING_VERSIONING_MODE,
                                               getDefaultTenant());

        // Well nothing should have changed
        // lets check that first SIP version is the latest
        sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());
        // check that session has been notified
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .incrementProductWaitingVersioningMode(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        // check that request is in state WAITING_VERSIONING_MODE, there is only this test request in DB so if there is one WAITING_VERSIONING_MODE, it is ours
        Assert.assertEquals("There should be one request in WAITING_VERSIONING_MODE state",
                            1,
                            ingestRequestRepository.countByState(InternalRequestState.WAITING_VERSIONING_MODE));
    }

    @Test
    public void testManualThenIncVersion() throws IllegalAccessException {
        // Init requests in WAITING_VERSIONING_MODE
        testManual();

        // Create job to set vesion mode state
        ChooseVersioningJob job = new ChooseVersioningJob();
        FieldUtils.writeField(job, "requestIterationLimit", 1000, true);
        FieldUtils.writeField(job, "ingestRequestService", ingestRequestService, true);
        FieldUtils.writeField(job, "requestService", requestService, true);

        ChooseVersioningRequestParameters filters = new ChooseVersioningRequestParameters();
        filters.setNewVersioningMode(VersioningMode.INC_VERSION);
        FieldUtils.writeField(job, "filters", filters, true);

        // Run job to set version mode to INC_VERSION
        job.run();

        ingestServiceTest.waitForAIP(2, 20000, AIPState.STORED, getDefaultTenant());
        // lets check that second SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, sips.length);
        Assert.assertTrue(String.format(
            "This SIP should be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[1].getVersion(),
            PROVIDER_ID), sips[1].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[1].getState());
        Assert.assertFalse(String.format(
            "This SIP should not be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[0].getVersion(),
            PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIPs
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, aips.length);
        Assert.assertTrue(String.format(
            "This AIP should be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[1].getVersion(),
            PROVIDER_ID), aips[1].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[1].getState());
        Assert.assertFalse(String.format(
            "This AIP should not be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[0].getVersion(),
            PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());
        // check session notifier
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductWaitingVersioningMode(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));
    }

    @Test
    public void testManualThenReplace() {
        testManual();
        // lets get that request in WAITING_VERSIONING_MODE and switch it to INC_VERSION
        ingestRequestRepository.findOne((root, query, cb) -> {
                                   Set<Predicate> predicates = Sets.newHashSet();
                                   predicates.add(cb.equal(root.get(STATE_ATTRIBUTE), InternalRequestState.WAITING_VERSIONING_MODE));
                                   return cb.and(predicates.toArray(new Predicate[predicates.size()]));
                               })
                               .ifPresent(request -> ingestRequestService.fromWaitingTo(Lists.newArrayList(request),
                                                                                        VersioningMode.REPLACE));

        ingestServiceTest.waitForAIP(2, 20000, AIPState.STORED, getDefaultTenant());
        // once the 2 AIPs are stored, we ask for the deletion of the old one, so lets wait for this deletion
        ingestServiceTest.waitForAIP(1, 20_000, AIPState.DELETED, getDefaultTenant());
        // lets check that second SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, sips.length);
        Assert.assertTrue(String.format(
            "This SIP should be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[1].getVersion(),
            PROVIDER_ID), sips[1].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[1].getState());
        Assert.assertFalse(String.format(
            "This SIP should not be the latest as it is version %s out of 2 SIP for providerId \"%s\"",
            sips[0].getVersion(),
            PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.DELETED),
                            SIPState.DELETED,
                            sips[0].getState());
        // lets check associated AIPs
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only two AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 2, aips.length);
        Assert.assertTrue(String.format(
            "This AIP should be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[1].getVersion(),
            PROVIDER_ID), aips[1].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[1].getState());
        Assert.assertFalse(String.format(
            "This AIP should not be the latest as it is version %s out of 2 AIP for providerId \"%s\"",
            aips[0].getVersion(),
            PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.DELETED),
                            AIPState.DELETED,
                            aips[0].getState());
        // check session notifier
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductWaitingVersioningMode(Mockito.any(IngestRequest.class));
        // 1 - for new version
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .decrementProductStorePending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(2))
               .incrementProductStoreSuccess(Mockito.any(IngestRequest.class));
        // 2 - for old version removal
        // there is the request from 2 IngestRequest, OAISDeletionCreatorRequest and OAISDeletionRequest that are deleted
        Mockito.verify(sessionNotifier, Mockito.times(4)).requestDeleted(Mockito.any(AbstractRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .productDeleted(Mockito.eq(SESSION_OWNER_0), Mockito.eq(SESSION_0), Mockito.anyCollection());
    }

    @Test
    public void testManualThenIgnore() {
        testManual();
        // lets get that request in WAITING_VERSIONING_MODE and switch it to INC_VERSION
        ingestRequestRepository.findOne((root, query, cb) -> {
            Set<Predicate> predicates = Sets.newHashSet();
            predicates.add(cb.equal(root.get(STATE_ATTRIBUTE), InternalRequestState.WAITING_VERSIONING_MODE));
            return cb.and(predicates.toArray(new Predicate[predicates.size()]));
        }).ifPresent(request -> ingestRequestService.fromWaitingTo(Lists.newArrayList(request), VersioningMode.IGNORE));

        ingestServiceTest.waitForIngestRequest(1, 20_000, InternalRequestState.IGNORED, getDefaultTenant());

        // Well nothing should have changed
        // lets check that first SIP version is the latest
        SIPEntity[] sips = sipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new SIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one SIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, sips.length);
        Assert.assertTrue(String.format("This SIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), sips[0].isLast());
        Assert.assertEquals(String.format("This SIP should be in state %s", SIPState.STORED),
                            SIPState.STORED,
                            sips[0].getState());
        // lets check associated AIP
        AIPEntity[] aips = aipRepository.findAllByProviderIdOrderByVersionAsc(PROVIDER_ID).toArray(new AIPEntity[0]);
        Assert.assertEquals(String.format("There should be only one AIP with providerId \"%s\" at this time",
                                          PROVIDER_ID), 1, aips.length);
        Assert.assertTrue(String.format("This AIP should be the latest as it is the only one for providerId \"%s\"",
                                        PROVIDER_ID), aips[0].isLast());
        Assert.assertEquals(String.format("This AIP should be in state %s", AIPState.STORED),
                            AIPState.STORED,
                            aips[0].getState());
        // check that request is in state IGNORE, there is only this test request in DB so if there is one IGNORED, it is ours
        Assert.assertEquals("There should be one request in IGNORED state",
                            1,
                            ingestRequestRepository.countByState(InternalRequestState.IGNORED));
        // check session notifier
        Mockito.verify(sessionNotifier, Mockito.times(1))
               .decrementProductWaitingVersioningMode(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .incrementProductGenerationPending(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(1)).incrementProductIgnored(Mockito.any(IngestRequest.class));
        Mockito.verify(sessionNotifier, Mockito.times(3))
               .decrementProductGenerationPending(Mockito.any(IngestRequest.class));
    }
}
