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
 * along with REGARDS. If not, see <http:www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ingest.service.session;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.request.ingest.IngestRequest;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import org.apache.commons.lang3.math.NumberUtils;
import org.assertj.core.util.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=session_notif",
                                   "eureka.client.enabled=false" },
                    locations = { "classpath:application-test.properties" })
public class SessionNotifierIT extends AbstractMultitenantServiceIT {

    @Autowired
    private SessionNotifier sessionNotifier;

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
        sipEntity = SIPEntity.build(getDefaultTenant(),
                                    IngestMetadata.build(sessionOwner,
                                                         session,
                                                         null,
                                                         ingestChain,
                                                         categories,
                                                         StorageMetadata.build("AWS", "/dir1/dir2/", new HashSet<>())),
                                    SIPDto.build(EntityType.DATA, providerId),
                                    1,
                                    SIPState.INGESTED);
        aipEntity1 = createAIPEntity();
        aipEntity2 = createAIPEntity();
        aips.add(aipEntity1);
        aips.add(aipEntity2);
        clearPublishedEvents();

        // init ingest request
        ingestRequest = new IngestRequest(UUID.randomUUID().toString());
        ingestRequest.setSessionOwner(sessionOwner);
        ingestRequest.setSession(session);
        ingestRequest.setAips(aips);
    }

    private AIPEntity createAIPEntity() {
        return AIPEntity.build(sipEntity,
                               AIPState.GENERATED,
                               AIPDto.build(sipEntity.getSip(),
                                            OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                                    EntityType.COLLECTION,
                                                                                    getDefaultTenant(),
                                                                                    1),
                                            Optional.ofNullable(sipEntity.getSipIdUrn()),
                                            providerId,
                                            sipEntity.getVersion()));
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
        Map<String, Long> result = getResultUsingNotifs(getPublishedEvents(2, StepPropertyUpdateRequestEvent.class));
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
    }

    @Test
    public void testGenerationSuccess() {
        sessionNotifier.incrementRequestCount(sessionOwner, session, 1);
        sessionNotifier.incrementProductGenerationPending(ingestRequest);
        sessionNotifier.decrementProductGenerationPending(ingestRequest);
        sessionNotifier.incrementProductStoreSuccess(ingestRequest);

        Map<String, Long> result = getResultUsingNotifs(getPublishedEvents(4, StepPropertyUpdateRequestEvent.class));
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

        Map<String, Long> result = getResultUsingNotifs(getPublishedEvents(4, StepPropertyUpdateRequestEvent.class));
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

        Map<String, Long> result = getResultUsingNotifs(getPublishedEvents(6, StepPropertyUpdateRequestEvent.class));
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

        Map<String, Long> result = getResultUsingNotifs(getPublishedEvents(6, StepPropertyUpdateRequestEvent.class));
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

        Map<String, Long> result = getResultUsingNotifs(getPublishedEvents(8, StepPropertyUpdateRequestEvent.class));
        Assert.assertEquals(1, (long) result.get(SessionNotifierPropertyEnum.TOTAL_REQUESTS.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REQUESTS_RUNNING.getName()));
        Assert.assertEquals(0, (long) result.get(SessionNotifierPropertyEnum.REFERENCED_PRODUCTS.getName()));
        Assert.assertEquals(2, (long) result.get(SessionNotifierPropertyEnum.DELETED_PRODUCTS.getName()));
        Assert.assertNull(result.get(SessionNotifierPropertyEnum.REQUESTS_ERRORS.getName()));
    }
}