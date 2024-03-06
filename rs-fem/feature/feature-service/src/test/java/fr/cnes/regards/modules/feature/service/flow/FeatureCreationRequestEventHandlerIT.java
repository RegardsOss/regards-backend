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
import fr.cnes.regards.modules.feature.dao.IFeatureCreationRequestRepository;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.dto.event.in.FeatureCreationRequestEvent;
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

/**
 * Test for {@link FeatureCreationRequestEventHandler} to verify the retry system.
 * <p>{@link fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest}s should be created after
 * receiving AMQP events with retry.</p>
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=feature_creation_handler_it",
                                   "regards.amqp.enabled=true" },
                    locations = { "classpath:retry.properties",
                                  "classpath:batch.properties",
                                  "classpath:metrics.properties" })
@ActiveProfiles(value = { "testAmqp", "noscheduler" })
public class FeatureCreationRequestEventHandlerIT extends AbstractFeatureMultitenantServiceIT {

    @Autowired
    private IAmqpAdmin amqpAdmin;

    @MockBean
    private IFeatureCreationRequestRepository featureCreationRequestRepo;

    @Test
    public void givenValidCreateEvents_whenPublishedWithRetry_thenRequestsCreated() {
        // GIVEN
        // create FeatureCreationRequestEvents
        int nbEvents = 5;
        List<FeatureCreationRequestEvent> events = super.initFeatureCreationRequestEvent(nbEvents, true, false);
        // Simulate temporary exceptions to activate the retry of messages
        Mockito.when(featureCreationRequestRepo.saveAll(ArgumentMatchers.any()))
               .thenThrow(new DataAccessResourceFailureException("test exception to make the batch fail on CREATE."))
               .thenThrow(new DataAccessResourceFailureException(
                   "test exception to make the batch fail on CREATE : retry 1."))
               .thenAnswer(ans -> {
                   // then make the batch success on retry 2
                   List<FeatureCreationRequest> registeredRequests = (List<FeatureCreationRequest>) ans.getArguments()[0];
                   Assertions.assertThat(registeredRequests).hasSize(nbEvents);
                   return registeredRequests;
               });

        // WHEN
        // publish messages
        publisher.publish(events);

        // THEN
        // Retry header has to be updated
        verifyRetryHeaderAfterXFailures(nbEvents,
                                        2,
                                        amqpAdmin.getSubscriptionQueueName(FeatureCreationRequestEventHandler.class,
                                                                           Target.ONE_PER_MICROSERVICE_TYPE),
                                        amqpAdmin.getRetryExchangeName());
    }

}
