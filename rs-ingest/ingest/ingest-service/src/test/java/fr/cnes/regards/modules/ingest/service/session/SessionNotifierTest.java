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
package fr.cnes.regards.modules.ingest.service.session;

import com.google.common.collect.Lists;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionMonitoringEvent;
import fr.cnes.regards.modules.sessionmanager.domain.event.SessionNotificationOperator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_notif" })
public class SessionNotifierTest extends AbstractMultitenantServiceTest {
    @Autowired
    private SessionNotifier sessionNotifier;

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

    @Before
    public void init() {
        sipEntity = SIPEntity.build(getDefaultTenant(), IngestMetadata.build(sessionOwner, session,
                ingestChain, categories,StorageMetadata.build("AWS", "/dir1/dir2/", new HashSet<>())),
                SIP.build(EntityType.DATA, providerId),
                1, SIPState.INGESTED
        );
        aipEntity1 = AIPEntity.build(sipEntity, AIPState.GENERATED,
                AIP.build(sipEntity.getSip(),
                        UniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.COLLECTION, getDefaultTenant(), 1),
                        Optional.ofNullable(sipEntity.getSipIdUrn()),
                        providerId
                ));
        aipEntity2 = AIPEntity.build(sipEntity, AIPState.GENERATED,
                AIP.build(sipEntity.getSip(),
                        UniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.COLLECTION, getDefaultTenant(), 1),
                        Optional.ofNullable(sipEntity.getSipIdUrn()),
                        providerId
                ));
        Mockito.clearInvocations(publisher);
    }

    private Map<String, Long> getResultUsingNotifs(List<SessionMonitoringEvent> allValues) {
        Map<String, Long> result = new HashMap<>();
        for (SessionMonitoringEvent e : allValues) {
            if (e.getOperator() !=  SessionNotificationOperator.REPLACE) {
                Long previousValue = Optional.ofNullable(result.get(e.getProperty())).orElse(0L);
                if (e.getOperator() == SessionNotificationOperator.INC) {
                    result.put(e.getProperty(), previousValue + (Long)e.getValue());
                } else {
                    result.put(e.getProperty(), previousValue - (Long)e.getValue());
                }
            }
        }
        return result;
    }

    @Test
    public void testCreationFail() {
        sessionNotifier.notifySIPCreationFailed(sipEntity);

        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);

        Mockito.verify(publisher, Mockito.times(1)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifier.PROPERTY_SIP_ERROR));
    }


    @Test
    public void testStoreFail() {
        sessionNotifier.notifySIPCreated(sipEntity);
        sessionNotifier.notifyAIPCreated(Lists.newArrayList(aipEntity1, aipEntity2));

        sessionNotifier.notifySIPStorageFailed(sipEntity);
        sessionNotifier.notifyAIPStorageFailed(aipEntity1);
        sessionNotifier.notifyAIPStorageFailed(aipEntity2);

        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);

        Mockito.verify(publisher, Mockito.times(8)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifier.PROPERTY_SIP_ERROR));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTING));

        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_GENERATED));
        Assert.assertEquals(2, (long) result.get(SessionNotifier.PROPERTY_AIP_ERROR));
    }



    @Test
    public void testStoreSucceed() {
        sessionNotifier.notifySIPCreated(sipEntity);
        sessionNotifier.notifyAIPCreated(Lists.newArrayList(aipEntity1, aipEntity2));

        sipEntity.setState(SIPState.STORED);
        aipEntity1.setState(AIPState.STORED);
        aipEntity2.setState(AIPState.STORED);

        sessionNotifier.notifySIPStored(sipEntity);
        sessionNotifier.notifyAIPsStored(Lists.newArrayList(aipEntity1, aipEntity2));

        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);

        Mockito.verify(publisher, Mockito.times(6)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTING));
        Assert.assertEquals(1, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTED));

        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_GENERATED));
        Assert.assertEquals(2, (long) result.get(SessionNotifier.PROPERTY_AIP_STORED));
    }

    @Test
    public void testDeleteSucceed() {
        sessionNotifier.notifySIPCreated(sipEntity);
        sessionNotifier.notifyAIPCreated(Lists.newArrayList(aipEntity1, aipEntity2));

        sipEntity.setState(SIPState.STORED);
        aipEntity1.setState(AIPState.STORED);
        aipEntity2.setState(AIPState.STORED);

        sessionNotifier.notifySIPStored(sipEntity);
        sessionNotifier.notifyAIPsStored(Lists.newArrayList(aipEntity1, aipEntity2));

        sessionNotifier.notifySIPDeleting(sipEntity);
        sessionNotifier.notifyAIPDeleting(Sets.newLinkedHashSet(aipEntity1, aipEntity2));

        aipEntity1.setState(AIPState.DELETED);
        aipEntity2.setState(AIPState.DELETED);
        sipEntity.setState(SIPState.DELETED);

        sessionNotifier.notifySIPDeleted(sipEntity);
        sessionNotifier.notifyAIPDeleted(Sets.newLinkedHashSet(aipEntity1, aipEntity2));

        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);

        Mockito.verify(publisher, Mockito.times(12)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTING));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTED));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_DELETING));

        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_GENERATED));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_STORED));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_DELETING));
    }


    @Test
    public void testDeleteFailed() {
        sessionNotifier.notifySIPCreated(sipEntity);
        sessionNotifier.notifyAIPCreated(Lists.newArrayList(aipEntity1, aipEntity2));

        sipEntity.setState(SIPState.STORED);
        aipEntity1.setState(AIPState.STORED);
        aipEntity2.setState(AIPState.STORED);

        sessionNotifier.notifySIPStored(sipEntity);
        sessionNotifier.notifyAIPsStored(Lists.newArrayList(aipEntity1, aipEntity2));

        sessionNotifier.notifySIPDeleting(sipEntity);
        sessionNotifier.notifyAIPDeleting(Sets.newLinkedHashSet(aipEntity1, aipEntity2));

        aipEntity1.setState(AIPState.DELETED);
        aipEntity2.setState(AIPState.DELETED);
        sipEntity.setState(SIPState.DELETED);

        sessionNotifier.notifySIPDeletionFailed(sipEntity);
        sessionNotifier.notifyAIPDeletionFailed(Sets.newLinkedHashSet(aipEntity1, aipEntity2));

        ArgumentCaptor<SessionMonitoringEvent> argumentCaptor = ArgumentCaptor.forClass(SessionMonitoringEvent.class);

        Mockito.verify(publisher, Mockito.times(14)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTING));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_INGESTED));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_SIP_DELETING));
        Assert.assertEquals(1, (long) result.get(SessionNotifier.PROPERTY_SIP_ERROR));

        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_GENERATED));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_STORED));
        Assert.assertEquals(0, (long) result.get(SessionNotifier.PROPERTY_AIP_DELETING));
        Assert.assertEquals(2, (long) result.get(SessionNotifier.PROPERTY_AIP_ERROR));
    }
}
