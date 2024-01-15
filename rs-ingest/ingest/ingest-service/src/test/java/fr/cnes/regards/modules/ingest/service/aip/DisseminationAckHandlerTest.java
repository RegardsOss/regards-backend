/*

 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPUpdateRequestRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.DisseminationInfo;
import fr.cnes.regards.modules.ingest.domain.request.AbstractRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateDisseminationTask;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateRequest;
import fr.cnes.regards.modules.ingest.domain.request.update.AIPUpdateState;
import fr.cnes.regards.modules.ingest.domain.request.update.AbstractAIPUpdateTask;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.request.event.DisseminationAckEvent;
import fr.cnes.regards.modules.ingest.service.request.AIPUpdateRequestService;
import fr.cnes.regards.modules.ingest.service.request.IRequestService;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.MockitoAnnotations.openMocks;

/**
 * Test for {@link DisseminationAckHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class DisseminationAckHandlerTest {

    public static final String LABEL_TEST = "label-test";

    @Spy
    private ISubscriber subscriber;

    @Mock
    private IAIPUpdateRequestRepository aipUpdateRequestRepository;

    @Mock
    private IAbstractRequestRepository abstractRequestRepository;

    @Mock
    private IRequestService requestService;

    @Mock
    private AIPService aipService;

    @Mock
    private ISIPRepository sipRepository;

    private DisseminationAckHandler disseminationAckHandler;

    private AutoCloseable closable;

    private OaisUniformResourceName resourceName;

    private AIPUpdateRequestService updateService;

    private final List<DisseminationAckEvent> disseminationAckMessages = new ArrayList<>();

    @Before
    public void init() {
        closable = openMocks(this);
        updateService = new AIPUpdateRequestService(aipUpdateRequestRepository,
                                                    abstractRequestRepository,
                                                    requestService,
                                                    sipRepository);
        disseminationAckHandler = new DisseminationAckHandler(subscriber, updateService, aipService);
        initData();
        initMockBehaviours();
    }

    private void initData() {
        resourceName = OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP, EntityType.DATA, "tenant", 2);
        DisseminationAckEvent disseminationAckEvent = new DisseminationAckEvent(resourceName.toString(), LABEL_TEST);
        disseminationAckMessages.add(disseminationAckEvent);
    }

    private void initMockBehaviours() {
        AIPEntity aipEntity = AIPEntity.build(AIPState.STORED,
                                              AIPDto.build(EntityType.DATA,
                                                           resourceName,
                                                           Optional.empty(),
                                                           "providerId",
                                                           5));
        Mockito.when(aipService.findByAipIds(any())).thenReturn(new ArrayList<AIPEntity>(List.of(aipEntity)));
        Mockito.when(aipUpdateRequestRepository.findRunningRequestAndAipIdIn(any())).thenReturn(new ArrayList<>());
    }

    @Test
    @Purpose("Test if update requests are successfully saved from update dissemination request from amqp")
    public void handle_submission_requests_success() {

        AtomicReference<List<AbstractRequest>> refRequestsCreated = new AtomicReference<>(new ArrayList<>());
        AtomicReference<List<Multimap<AIPEntity, AbstractAIPUpdateTask>>> maps = new AtomicReference<>(new ArrayList<>());

        Mockito.when(requestService.scheduleRequests(any())).thenAnswer(res -> {
            refRequestsCreated.set(res.getArgument(0));
            return 0;
        });

        disseminationAckHandler.handleBatch(disseminationAckMessages);
        Assertions.assertThat(refRequestsCreated.get()).hasSize(1);

        //Check if the request contains the right dissemination info
        for (AbstractRequest request : refRequestsCreated.get()) {
            AbstractAIPUpdateTask updateTask = ((AIPUpdateRequest) request).getUpdateTask();
            Assertions.assertThat(updateTask.getState()).isEqualTo(AIPUpdateState.READY);
            List<DisseminationInfo> disseminationInfo = ((AIPUpdateDisseminationTask) updateTask).getDisseminationInfoUpdates();
            Assertions.assertThat(disseminationInfo.get(0).getLabel()).isEqualTo(LABEL_TEST);
            Assertions.assertThat(disseminationInfo.get(0).getAckDate()).isNotNull();
        }
    }

    @After
    public void closeMocks() throws Exception {
        closable.close();
    }

}
