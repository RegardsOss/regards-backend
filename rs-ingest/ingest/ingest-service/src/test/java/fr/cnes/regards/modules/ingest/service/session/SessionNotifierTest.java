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
 * along with REGARDS. If not, see <http:www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.math.NumberUtils;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestPropertySource;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(
        properties = { "spring.jpa.properties.hibernate.default_schema=session_notif", "eureka.client.enabled=false" },
        locations = { "classpath:application-test.properties" })
public class SessionNotifierTest extends AbstractMultitenantServiceTest {

    @Autowired
    private SessionNotifier sessionNotifier;

    @SpyBean
    private IPublisher publisher;

    private static String sessionOwner = "NASA";

    private static String session = "session d'ingestion";

    private static String ingestChain = "ingest chain";

    private static String providerId = "provider 1";

    private static Set<String> categories = Sets.newLinkedHashSet("CAT 1", "CAT 2");

    private static SIPEntity sipEntity;

    private static AIPEntity aipEntity1;

    private static AIPEntity aipEntity2;

    private final ArrayList<AIPEntity> aips = new ArrayList<>();

    private IngestRequest ingestRequest;

    @Before
    public void init() {
        sipEntity = SIPEntity.build(getDefaultTenant(), IngestMetadata
                                            .build(sessionOwner, session, ingestChain, categories,
                                                   StorageMetadata.build("AWS", "/dir1/dir2/", new HashSet<>())),
                                    SIP.build(EntityType.DATA, providerId), 1, SIPState.INGESTED);
        aipEntity1 = AIPEntity.build(sipEntity, AIPState.GENERATED, AIP.build(sipEntity.getSip(),
                                                                              OaisUniformResourceName.pseudoRandomUrn(
                                                                                      OAISIdentifier.AIP,
                                                                                      EntityType.COLLECTION,
                                                                                      getDefaultTenant(), 1),
                                                                              Optional.ofNullable(
                                                                                      sipEntity.getSipIdUrn()),
                                                                              providerId, sipEntity.getVersion()));
        aipEntity2 = AIPEntity.build(sipEntity, AIPState.GENERATED, AIP.build(sipEntity.getSip(),
                                                                              OaisUniformResourceName.pseudoRandomUrn(
                                                                                      OAISIdentifier.AIP,
                                                                                      EntityType.COLLECTION,
                                                                                      getDefaultTenant(), 1),
                                                                              Optional.ofNullable(
                                                                                      sipEntity.getSipIdUrn()),
                                                                              providerId, sipEntity.getVersion()));
        aips.add(aipEntity1);
        aips.add(aipEntity2);
        Mockito.clearInvocations(publisher);

        // init ingest request
        ingestRequest = new IngestRequest();
        ingestRequest.setSessionOwner(sessionOwner);
        ingestRequest.setSession(session);
        ingestRequest.setAips(aips);
    }

    private Map<String, Long> getResultUsingNotifs(List<StepPropertyUpdateRequestEvent> allValues) {
        Map<String, Long> result = new HashMap<>();
        for (StepPropertyUpdateRequestEvent e : allValues) {
            if (e.getType() != StepPropertyEventTypeEnum.VALUE) {
                String propertyUpdated = e.getStepProperty().getStepPropertyInfo().getProperty();
                Long previousValue = Optional.ofNullable(result.get(propertyUpdated)).orElse(0L);
                String currentValue = e.getStepProperty().getStepPropertyInfo().getValue();
                if (e.getType() == StepPropertyEventTypeEnum.INC) {
                    result.put(propertyUpdated, previousValue + NumberUtils.toLong(currentValue));
                } else {
                    result.put(propertyUpdated, previousValue - NumberUtils.toLong(currentValue));
                }
            }
        }
        return result;
    }

    @Test
    public void testGenerationStart() {
        // launch tests
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        // check results
        ArgumentCaptor<StepPropertyUpdateRequestEvent> argumentCaptor = ArgumentCaptor.forClass(StepPropertyUpdateRequestEvent.class);
        Mockito.verify(publisher, Mockito.times(2)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
    }

    @Test
    public void testGenerationSuccess() {
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        sessionNotifier.decrementProductGenerationPending(ingestRequest);
        sessionNotifier.incrementProductStoreSuccess(ingestRequest);

        ArgumentCaptor<StepPropertyUpdateRequestEvent> argumentCaptor = ArgumentCaptor.forClass(StepPropertyUpdateRequestEvent.class);
        Mockito.verify(publisher, Mockito.times(4)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
        Assert.assertEquals(aips.size(), (long) result.get(SessionNotifierPropertyEnum.REFERENCED_PRODUCTS.getName()));
    }

    @Test
    public void testGenerationFail() {
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        sessionNotifier.decrementProductGenerationPending(ingestRequest);
        sessionNotifier.incrementProductGenerationError(ingestRequest);

        ArgumentCaptor<StepPropertyUpdateRequestEvent> argumentCaptor = ArgumentCaptor.forClass(StepPropertyUpdateRequestEvent.class);
        Mockito.verify(publisher, Mockito.times(4)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName()));
    }

    @Test
    public void testStoreFail() {
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        sessionNotifier.decrementProductGenerationPending(ingestRequest);
        sessionNotifier.incrementProductStorePending(ingestRequest);
        sessionNotifier.decrementProductStorePending(ingestRequest);
        sessionNotifier.incrementProductStoreError(ingestRequest);

        ArgumentCaptor<StepPropertyUpdateRequestEvent> argumentCaptor = ArgumentCaptor.forClass(StepPropertyUpdateRequestEvent.class);
        Mockito.verify(publisher, Mockito.times(6)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName()));
    }

    @Test
    public void testStoreSucceed() {
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        sessionNotifier.decrementProductGenerationPending(ingestRequest);
        sessionNotifier.incrementProductStorePending(ingestRequest);
        sessionNotifier.decrementProductStorePending(ingestRequest);
        sessionNotifier.incrementProductStoreSuccess(ingestRequest);

        ArgumentCaptor<StepPropertyUpdateRequestEvent> argumentCaptor = ArgumentCaptor.forClass(StepPropertyUpdateRequestEvent.class);
        Mockito.verify(publisher, Mockito.times(6)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
        Assert.assertEquals(aips.size(), (long) result.get(SessionNotifierPropertyEnum.REFERENCED_PRODUCTS.getName()));
        Assert.assertNull(result.get(SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName()));

    }

    @Test
    public void testDeletion() {
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        sessionNotifier.decrementProductGenerationPending(ingestRequest);
        sessionNotifier.incrementProductStorePending(ingestRequest);
        sessionNotifier.decrementProductStorePending(ingestRequest);
        sessionNotifier.incrementProductStoreSuccess(ingestRequest);
        aipEntity1.setState(AIPState.STORED);
        aipEntity2.setState(AIPState.STORED);
        sipEntity.setState(SIPState.STORED);
        sessionNotifier.productDeleted(sessionOwner, session, aips);

        ArgumentCaptor<StepPropertyUpdateRequestEvent> argumentCaptor = ArgumentCaptor.forClass(StepPropertyUpdateRequestEvent.class);
        Mockito.verify(publisher, Mockito.times(8)).publish(argumentCaptor.capture());
        Map<String, Long> result = getResultUsingNotifs(argumentCaptor.getAllValues());
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REFERENCED_PRODUCTS.getName()));
        Assert.assertEquals(2, (long) result.get(SessionNotifierPropertyEnum.DELETED_PRODUCTS.getName()));
        Assert.assertNull(result.get(SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName()));
    }
}