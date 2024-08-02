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
package fr.cnes.regards.modules.workermanager.service.flow;

import fr.cnes.regards.modules.workercommon.dto.WorkerResponseStatus;
import fr.cnes.regards.modules.workermanager.dao.IRequestRepository;
import fr.cnes.regards.modules.workermanager.domain.request.Request;
import fr.cnes.regards.modules.workermanager.dto.requests.RequestStatus;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test for {@link WorkerResponseHandlerRetryIT} to verify the retry system.
 * <p>{@link Request}s should be updated after receiving AMQP events with retry.</p>
 */
@ActiveProfiles(value = { "testAmqp", "nojobs" })
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_responses_retry_it",
                                   "regards.amqp.enabled=true",
                                   "regards.workermanager.worker.response.bulk.size=1000" },
                    locations = { "classpath:application-test.properties", "classpath:retry.properties" })
public class WorkerResponseHandlerRetryIT extends AbstractWorkerManagerIT {

    @MockBean
    private IRequestRepository requestRepository;

    @Test
    public void givenValidRequestEvents_whenPublishedWithRetry_thenRequestsUpdated() {
        // GIVEN
        // Mock initial request previously saved by the worker
        // return multiple times Request because this object is not immutable and modified in the RequestService
        Mockito.when(requestRepository.findByRequestIdIn(ArgumentMatchers.any()))
               .thenReturn(createInitialRequest())
               .thenReturn(createInitialRequest())
               .thenReturn(createInitialRequest());
        // Simulate temporary exceptions on response events to activate the retry of messages
        Mockito.when(requestRepository.saveAll(createInitialRequest()))
               .thenThrow(new DataAccessResourceFailureException("test exception to make the batch fail on RESPONSE."))
               .thenThrow(new DataAccessResourceFailureException(
                   "test exception to make the batch fail on RESPONSE : retry 1."))
               .thenAnswer(ans -> {
                   List<Request> registeredRequests = (List<Request>) ans.getArguments()[0];
                   Assertions.assertThat(registeredRequests).as("Expected only one request to be saved.").hasSize(1);
                   Assertions.assertThat(registeredRequests.get(0).getStatus())
                             .as("Requests status invalid")
                             .isEqualTo(RequestStatus.RUNNING);
                   Assertions.assertThat(responseMock.getEvents())
                             .as("Invalid number of response event sent")
                             .isEmpty();
                   return registeredRequests;
               });
        // Mock ending process
        Mockito.doNothing().when(requestRepository).deleteAllInBatch(ArgumentMatchers.any());

        // WHEN
        // Send new response event to worker
        publishWorkerResponse(createWorkerResponseEvent("1", WorkerResponseStatus.RUNNING));

        // THEN
        // check that message has been retried multiple times
        verifyRetryHeaderAfterXFailures(1,
                                        2,
                                        requestService.getWorkerResponseQueueName(),
                                        amqpAdmin.getRetryExchangeName());
        // session should be updated only one time
        sessionHelper.checkSession(5,
                                   TimeUnit.MINUTES,
                                   2,
                                   DEFAULT_SOURCE,
                                   DEFAULT_SESSION,
                                   DEFAULT_WORKER,
                                   0,
                                   1,
                                   0,
                                   -1,
                                   0,
                                   0,
                                   0,
                                   0,
                                   0);
    }

    private List<Request> createInitialRequest() {
        List<Request> initialRequest = new ArrayList<>();
        initialRequest.add(createRequest("1", RequestStatus.DISPATCHED));
        return initialRequest;
    }

}
