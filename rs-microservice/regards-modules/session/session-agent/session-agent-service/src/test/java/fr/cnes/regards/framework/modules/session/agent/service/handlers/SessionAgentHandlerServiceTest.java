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
package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.modules.session.agent.dao.IStepPropertyUpdateRequestRepository;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyEventTypeEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.events.StepPropertyUpdateRequestEvent;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;
import fr.cnes.regards.framework.modules.session.agent.domain.update.StepPropertyUpdateRequest;
import fr.cnes.regards.framework.modules.session.commons.dao.ISnapshotProcessRepository;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Stephane Cortine
 */
@RunWith(MockitoJUnitRunner.class)
public class SessionAgentHandlerServiceTest {

    @InjectMocks
    private SessionAgentHandlerService sessionAgentHandlerService;

    @Mock
    private IStepPropertyUpdateRequestRepository stepPropertyUpdateRequestRepository;

    @Mock
    private ISnapshotProcessRepository snapshotProcessRepository;

    @Test
    public void test_createStepRequests_with_groupBy() {
        // Given
        ReflectionTestUtils.setField(sessionAgentHandlerService, "groupByStepPropertyUpdateRequestEvt", true);

        // When
        Set<String> sourcesToBeUpdated = sessionAgentHandlerService.createStepRequests(
            createStepPropertyUpdateRequestEvents());

        // Then
        ArgumentCaptor<List<StepPropertyUpdateRequest>> StepPropertyUpdateReqsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(stepPropertyUpdateRequestRepository, Mockito.times(1))
               .saveAll(StepPropertyUpdateReqsCaptor.capture());

        Assert.assertNotNull(sourcesToBeUpdated);
        Assert.assertEquals(1, sourcesToBeUpdated.size());

        Assert.assertEquals(2, StepPropertyUpdateReqsCaptor.getValue().size());
        Assert.assertEquals("3", StepPropertyUpdateReqsCaptor.getValue().get(0).getStepPropertyInfo().getValue());
        Assert.assertEquals("3", StepPropertyUpdateReqsCaptor.getValue().get(1).getStepPropertyInfo().getValue());
    }

    @Test
    public void test_createStepRequests_without_groupBy() {
        // Given
        ReflectionTestUtils.setField(sessionAgentHandlerService, "groupByStepPropertyUpdateRequestEvt", false);

        // When
        Set<String> sourcesToBeUpdated = sessionAgentHandlerService.createStepRequests(
            createStepPropertyUpdateRequestEvents());

        // Then
        ArgumentCaptor<List<StepPropertyUpdateRequest>> StepPropertyUpdateReqsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(stepPropertyUpdateRequestRepository, Mockito.times(1))
               .saveAll(StepPropertyUpdateReqsCaptor.capture());

        Assert.assertNotNull(sourcesToBeUpdated);
        Assert.assertEquals(1, sourcesToBeUpdated.size());

        Assert.assertEquals(6, StepPropertyUpdateReqsCaptor.getValue().size());
        Assert.assertEquals("1", StepPropertyUpdateReqsCaptor.getValue().get(0).getStepPropertyInfo().getValue());
        Assert.assertEquals("1", StepPropertyUpdateReqsCaptor.getValue().get(1).getStepPropertyInfo().getValue());
        Assert.assertEquals("1", StepPropertyUpdateReqsCaptor.getValue().get(2).getStepPropertyInfo().getValue());
        Assert.assertEquals("1", StepPropertyUpdateReqsCaptor.getValue().get(3).getStepPropertyInfo().getValue());
        Assert.assertEquals("1", StepPropertyUpdateReqsCaptor.getValue().get(4).getStepPropertyInfo().getValue());
        Assert.assertEquals("1", StepPropertyUpdateReqsCaptor.getValue().get(5).getStepPropertyInfo().getValue());
    }

    // ---------------------
    // -- UTILITY METHODS --
    // ---------------------

    private List<StepPropertyUpdateRequestEvent> createStepPropertyUpdateRequestEvents() {
        List<StepPropertyUpdateRequestEvent> stepPropertyUpdateRequestEvts = new ArrayList<>();

        StepProperty stepPropertyInc = new StepProperty("storage",
                                                        "source1",
                                                        "session1",
                                                        new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                             StepPropertyStateEnum.SUCCESS,
                                                                             "storeRequests",
                                                                             "1",
                                                                             true,
                                                                             false));

        stepPropertyUpdateRequestEvts.add(new StepPropertyUpdateRequestEvent(stepPropertyInc,
                                                                             StepPropertyEventTypeEnum.INC));
        stepPropertyUpdateRequestEvts.add(new StepPropertyUpdateRequestEvent(stepPropertyInc,
                                                                             StepPropertyEventTypeEnum.INC));
        stepPropertyUpdateRequestEvts.add(new StepPropertyUpdateRequestEvent(stepPropertyInc,
                                                                             StepPropertyEventTypeEnum.INC));

        StepProperty stepPropertyDec = new StepProperty("storage",
                                                        "source1",
                                                        "session1",
                                                        new StepPropertyInfo(StepTypeEnum.STORAGE,
                                                                             StepPropertyStateEnum.RUNNING,
                                                                             "requestRunning",
                                                                             "1",
                                                                             false,
                                                                             false));

        stepPropertyUpdateRequestEvts.add(new StepPropertyUpdateRequestEvent(stepPropertyDec,
                                                                             StepPropertyEventTypeEnum.DEC));
        stepPropertyUpdateRequestEvts.add(new StepPropertyUpdateRequestEvent(stepPropertyDec,
                                                                             StepPropertyEventTypeEnum.DEC));
        stepPropertyUpdateRequestEvts.add(new StepPropertyUpdateRequestEvent(stepPropertyDec,
                                                                             StepPropertyEventTypeEnum.DEC));

        return stepPropertyUpdateRequestEvts;
    }

}
