/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Ignore;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=session_notif", "eureka.client.enabled=false" },
        locations = { "classpath:application-test.properties" })
@Ignore
public class SessionNotifierTest extends AbstractMultitenantServiceTest {

    @SpyBean
    private IPublisher publisher;

    private static String sessionOwner = "NAASA";

    private static String session = "session d'ingestion";

    private static String ingestChain = "ingest chain";

    private static String providerId = "provider 1";

    private static Set<String> categories = Sets.newLinkedHashSet("CAT 1", "CAT 2");

    private static SIPEntity sipEntity;

    private static AIPEntity aipEntity1;

    private static AIPEntity aipEntity2;

    private final ArrayList<AIPEntity> aips = new ArrayList<>();

    @Before
    public void init() {
        sipEntity = SIPEntity.build(getDefaultTenant(),
                                    IngestMetadata.build(sessionOwner, session, ingestChain, categories,
                                                         StorageMetadata.build("AWS", "/dir1/dir2/", new HashSet<>())),
                                    SIP.build(EntityType.DATA, providerId), 1, SIPState.INGESTED);
        aipEntity1 = AIPEntity
                .build(sipEntity, AIPState.GENERATED,
                       AIP.build(sipEntity.getSip(),
                                 OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.COLLECTION,
                                                                         getDefaultTenant(), 1),
                                 Optional.ofNullable(sipEntity.getSipIdUrn()), providerId, sipEntity.getVersion()));
        aipEntity2 = AIPEntity
                .build(sipEntity, AIPState.GENERATED,
                       AIP.build(sipEntity.getSip(),
                                 OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.COLLECTION,
                                                                         getDefaultTenant(), 1),
                                 Optional.ofNullable(sipEntity.getSipIdUrn()), providerId, sipEntity.getVersion()));
        aips.add(aipEntity1);
        aips.add(aipEntity2);
        Mockito.clearInvocations(publisher);
    }

    /*@SuppressWarnings("unused")
    private Map<String, Long> getResultUsingNotifs(List<SessionMonitoringEvent> allValues) {
        Map<String, Long> result = new HashMap<>();
        for (SessionMonitoringEvent e : allValues) {
            if (e.getOperator() != SessionNotificationOperator.REPLACE) {
                Long previousValue = Optional.ofNullable(result.get(e.getProperty())).orElse(0L);
                if (e.getOperator() == SessionNotificationOperator.INC) {
                    result.put(e.getProperty(), previousValue + (Long) e.getValue());
                } else {
                    result.put(e.getProperty(), previousValue - (Long) e.getValue());
                }
            }
        }
        return result;
    }*/

    //    @Test
    //    public void testGenerationStart() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(2)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(1, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(1, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //    }
    //
    //    @Test
    //    public void testGenerationSuccess() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(5)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //    }
    //
    //    @Test
    //    public void testGenerationFail() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, new ArrayList<>());
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //
    //        Mockito.verify(publisher, Mockito.times(4)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(1, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(1, (long) result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_META_STORE_PENDING));
    //    }
    //
    //    @Test
    //    public void testStoreFail() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //        sessionNotifier.productStoreError(sessionOwner, session, aips);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(7)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_STORED));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_STORE_ERROR));
    //    }
    //
    //    @Test
    //    public void testStoreSucceed() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //        sessionNotifier.productStoreSuccess(sessionOwner, session, aips);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(7)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_STORED));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_STORE_ERROR));
    //    }
    //
    //    @Test
    //    public void testStoreMetaPending() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //        sessionNotifier.productStoreSuccess(sessionOwner, session, aips);
    //        sessionNotifier.productMetaStorePending(sessionOwner, session, aips);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(8)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_STORED));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_STORE_ERROR));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_META_STORE_PENDING));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_META_STORED));
    //    }
    //
    //    @Test
    //    public void testStoreMetaSucceed() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //        sessionNotifier.productStoreSuccess(sessionOwner, session, aips);
    //        sessionNotifier.productMetaStorePending(sessionOwner, session, aips);
    //        sessionNotifier.productMetaStoredSuccess(aipEntity1);
    //        sessionNotifier.productMetaStoredSuccess(aipEntity2);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(12)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_STORED));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_STORE_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_META_STORE_PENDING));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_META_STORED));
    //    }
    //
    //    @Test
    //    public void testStoreMetaError() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //        sessionNotifier.productStoreSuccess(sessionOwner, session, aips);
    //        sessionNotifier.productMetaStorePending(sessionOwner, session, aips);
    //        sessionNotifier.productMetaStoredSuccess(aipEntity1);
    //        sessionNotifier.productMetaStoredError(aipEntity2);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //        Mockito.verify(publisher, Mockito.times(12)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //        Assert.assertEquals(2, (long) result.get(SessionNotifier.PRODUCT_STORED));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_STORE_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_META_STORE_PENDING));
    //        Assert.assertEquals(1, (long) result.get(SessionNotifier.PRODUCT_META_STORED));
    //        Assert.assertEquals(1, (long) result.get(SessionNotifier.PRODUCT_META_STORE_ERROR));
    //    }
    //
    //    @Test
    //    public void testDeletion() {
    //        sessionNotifier.productsGranted(sessionOwner, session, 1);
    //        sessionNotifier.productGenerationStart(sessionOwner, session);
    //        sessionNotifier.productGenerationEnd(sessionOwner, session, aips);
    //        sessionNotifier.productStoreSuccess(sessionOwner, session, aips);
    //        aipEntity1.setState(AIPState.STORED);
    //        aipEntity2.setState(AIPState.STORED);
    //        sipEntity.setState(SIPState.STORED);
    //        sessionNotifier.productDeleted(sessionOwner, session, aips);
    //
    //        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);
    //
    //        Mockito.verify(publisher, Mockito.times(9)).publish(argumentCaptor.capture());
    //        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_COUNT));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_GEN_ERROR));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_GEN_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORE_PENDING));
    //        Assert.assertEquals(0, (long) result.get(SessionNotifier.PRODUCT_STORED));
    //        Assert.assertNull(result.get(SessionNotifier.PRODUCT_STORE_ERROR));
    //    }
}
