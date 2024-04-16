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
package fr.cnes.regards.modules.feature.service;

import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.service.request.IFeatureRequestService;
import fr.cnes.regards.modules.storage.domain.event.FileRequestType;
import fr.cnes.regards.modules.storage.domain.event.FileRequestsGroupEvent;
import fr.cnes.regards.modules.storage.domain.flow.FlowItemStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;

/**
 * Test class to check optimistic lock failures on storage response handlers.
 * OptimisticLock failure can happen on {@link fr.cnes.regards.modules.feature.domain.FeatureEntity} updates.
 *
 * @author Sébastien Binda
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_update",
                                   "regards.amqp.enabled=true",
                                   "regards.feature.delay.before.processing=1",
                                   "regards.feature.metrics.enabled=true" },
                    locations = { "classpath:regards_perf.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler", "noFemHandler" })
public class StorageListenerIT extends AbstractFeatureMultitenantServiceIT {

    @SpyBean
    private IFeatureRequestService featureRequestService;

    @SpyBean
    private IFeatureCreationRequestRepository fcrRepo;

    @Before
    public void init() {
        Mockito.reset(fcrRepo, featureRequestService);
    }

    @Test
    public void test_storage_response_handler_with_one_optimistic_lock_failure() throws InterruptedException {
        Mockito.doThrow(OptimisticLockingFailureException.class)
               .doAnswer(InvocationOnMock::callRealMethod)
               .when(featureRequestService)
               .handleStorageSuccess(Mockito.anySet());

        FileRequestsGroupEvent event = FileRequestsGroupEvent.build("group_id",
                                                                    FileRequestType.STORAGE,
                                                                    FlowItemStatus.SUCCESS,
                                                                    new ArrayList<>());
        publisher.publish(event);
        Thread.sleep(5_000);
        Mockito.verify(fcrRepo, Mockito.times(1)).findByGroupIdIn(Mockito.anySet());
    }

    @Test
    public void test_storage_response_handler_with_too_many_optimistic_lock_failure() throws InterruptedException {
        Mockito.doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .doThrow(OptimisticLockingFailureException.class)
               .when(featureRequestService)
               .handleStorageSuccess(Mockito.anySet());

        FileRequestsGroupEvent event = FileRequestsGroupEvent.build("group_id",
                                                                    FileRequestType.STORAGE,
                                                                    FlowItemStatus.SUCCESS,
                                                                    new ArrayList<>());
        publisher.publish(event);
        Thread.sleep(2_000);
        Mockito.verify(fcrRepo, Mockito.times(0)).findByGroupIdIn(Mockito.anySet());
    }

}
