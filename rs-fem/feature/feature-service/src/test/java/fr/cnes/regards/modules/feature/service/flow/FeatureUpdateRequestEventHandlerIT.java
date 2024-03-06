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
package fr.cnes.regards.modules.feature.service.flow;

import fr.cnes.regards.framework.amqp.configuration.IAmqpAdmin;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.feature.dao.IFeatureUpdateRequestRepository;
import fr.cnes.regards.modules.feature.domain.AbstractFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest;
import fr.cnes.regards.modules.feature.service.AbstractFeatureMultitenantServiceIT;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Test for {@link FeatureUpdateRequestEventHandler} to verify the retry system.
 * <p>{@link fr.cnes.regards.modules.feature.domain.request.FeatureUpdateRequest}s should be created after
 * receiving AMQP events with retry.</p>
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_update_handler_it",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:retry.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class FeatureUpdateRequestEventHandlerIT extends AbstractFeatureMultitenantServiceIT {

    @MockBean
    protected IFeatureUpdateRequestRepository featureUpdateRequestRepo;

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @Test
    public void givenValidUpdateEvents_whenPublishedWithRetry_thenRequestsCreated() throws InterruptedException {
        // GIVEN
        // Create features
        int nbEvents = 6;
        prepareCreationTestData(false, nbEvents, true, true, false);
        // Simulate temporary exception to activate the retry of messages
        Mockito.when(featureUpdateRequestRepo.saveAll(ArgumentMatchers.any()))
               .thenThrow(new DataAccessResourceFailureException("test exception to make the batch fail on UPDATE."))
               .thenAnswer(ans -> {
                   List<FeatureUpdateRequest> registeredRequests = (List<FeatureUpdateRequest>) ans.getArguments()[0];
                   Assertions.assertThat(registeredRequests).hasSize(nbEvents);
                   return registeredRequests;
               });

        // WHEN
        // publish update events
        publisher.publish(prepareUpdateRequests(this.featureRepo.findAll()
                                                                .stream()
                                                                .map(AbstractFeatureEntity::getUrn)
                                                                .collect(Collectors.toList())));

        // THEN
        // Retry header has to be updated
        verifyRetryHeaderAfterXFailures(nbEvents,
                                        1,
                                        amqpAdmin.getSubscriptionQueueName(FeatureUpdateRequestEventHandler.class,
                                                                           Target.ONE_PER_MICROSERVICE_TYPE),
                                        amqpAdmin.getRetryExchangeName());
    }

}
