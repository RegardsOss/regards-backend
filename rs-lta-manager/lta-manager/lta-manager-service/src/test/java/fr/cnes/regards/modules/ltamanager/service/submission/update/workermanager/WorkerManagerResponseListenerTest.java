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
package fr.cnes.regards.modules.ltamanager.service.submission.update.workermanager;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionStatus;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmittedProduct;
import fr.cnes.regards.modules.ltamanager.dto.submission.LtaDataType;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.util.MimeType;
import org.springframework.validation.Validator;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;

/**
 * Test for {@link WorkerManagerResponseListener}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class WorkerManagerResponseListenerTest {

    private static final String OWNER = "owner";

    @Mock
    private ISubmissionRequestRepository requestRepository;

    @Spy
    private ISubscriber subscriber;

    @Spy
    private IPublisher publisher;

    @Spy
    private Validator validator;

    // class under test
    private WorkerManagerResponseListener responseListener;

    private final List<ResponseEvent> responseEvents = new ArrayList<>();

    private final List<String> responsesInErrorIds = new ArrayList<>();

    @Before
    public void init() {
        WorkerManagerResponseService responseService = new WorkerManagerResponseService(requestRepository);
        responseListener = new WorkerManagerResponseListener(responseService,
                                                             requestRepository,
                                                             subscriber,
                                                             publisher,
                                                             validator);
        initResponseEvents();
    }

    @Test
    @Purpose(
        "Check that submission requests are successfully updated following the receiving of worker response events.")
    public void update_request_success() {
        // ---- GIVEN ----
        // Response events init
        // Mock database behaviour to simulate request ids exist
        Mockito.when(requestRepository.findIdsByCorrelationIdInAndStatesIn(anyList(), anyList()))
               .thenReturn(responseEvents.stream()
                                         .map(res -> (String) res.getMessageProperties()
                                                                 .getHeader(EventHeadersHelper.REQUEST_ID_HEADER))
                                         .toList());

        Mockito.when(requestRepository.findSubmissionRequestByCorrelationId(any()))
               .thenReturn(Optional.of(simulateProductRequest()));

        // ---- WHEN ----
        // Responses events are sent to the worker manager listener
        responseListener.handleBatch(responseEvents);

        // --- THEN ----
        // Check submission requests are updated with correct corresponding status
        Mockito.verify(requestRepository, times(5)).updateRequestState(any(), any(), any(), any());
        for (ResponseEvent responseEvent : responseEvents) {
            String reqId = responseEvent.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER);
            switch (responseEvent.getState()) {
                case GRANTED -> Mockito.verify(requestRepository)
                                       .updateRequestState(eq(reqId),
                                                           eq(SubmissionRequestState.GENERATION_PENDING),
                                                           eq(null),
                                                           any(OffsetDateTime.class));

                case SUCCESS -> Mockito.verify(requestRepository)
                                       .updateRequestState(eq(reqId),
                                                           eq(SubmissionRequestState.GENERATED),
                                                           eq(null),
                                                           any(OffsetDateTime.class));
                case INVALID_CONTENT, ERROR, SKIPPED -> Mockito.verify(requestRepository)
                                                               .updateRequestState(eq(reqId),
                                                                                   eq(SubmissionRequestState.GENERATION_ERROR),
                                                                                   eq(StringUtils.join(responseEvent.getMessage(),
                                                                                                       " | ")),
                                                                                   any(OffsetDateTime.class));
                case DELAYED -> { // DELAYED state is not processed as there is nothing to do
                }
            }
        }
        //Check message were sent on ERROR, INVALID_CONTENT and SKIPPED
        ArgumentCaptor<List<SubmissionResponseDtoEvent>> captorPublished = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, times(1)).publish(captorPublished.capture());
        List<SubmissionResponseDtoEvent> capturedPublishedEvents = captorPublished.getAllValues()
                                                                                  .stream()
                                                                                  .flatMap(List::stream)
                                                                                  .toList();

        Assertions.assertEquals(3, capturedPublishedEvents.size(), "There should be 3 responses sent");
        Assertions.assertTrue(capturedPublishedEvents.stream()
                                                     .allMatch(event -> event.getResponseStatus()
                                                                        == SubmissionResponseStatus.ERROR),
                              "All responses should have denied status");
        Assertions.assertTrue(capturedPublishedEvents.stream()
                                                     .anyMatch(event -> responsesInErrorIds.contains(event.getCorrelationId())),
                              "All responses should have the id of a responseEvent in error");

    }

    @Test
    @Purpose("Check that no submission requests are updated if response events received do not correspond to any "
             + "request")
    public void update_request_no_request_update() {
        // ---- GIVEN ----
        // Response events init

        // ---- WHEN ----
        // Responses events are sent to the worker manager listener
        responseListener.handleBatch(responseEvents);

        // --- THEN ----
        // Check no submission requests are updated
        Mockito.verify(requestRepository, times(0)).updateRequestState(any(), any(), any(), any());
    }

    private SubmissionRequest simulateProductRequest() {
        return new SubmissionRequest("cid",
                                     "owner",
                                     "session",
                                     false,
                                     new SubmissionStatus(),
                                     new SubmittedProduct("dt",
                                                          "model",
                                                          Path.of("/tmp"),
                                                          new SubmissionRequestDto("cid",
                                                                                   "id",
                                                                                   "dt",
                                                                                   List.of(new ProductFileDto(
                                                                                       LtaDataType.RAWDATA,
                                                                                       "",
                                                                                       "",
                                                                                       "",
                                                                                       MimeType.valueOf(
                                                                                           "application/octet-stream"))))),
                                     null,
                                     null,
                                     null);
    }

    private void initResponseEvents() {
        // Init worker response events with status
        for (ResponseStatus status : ResponseStatus.values()) {
            String reqId = UUID.randomUUID().toString();
            ResponseEvent event = ResponseEvent.build(status, reqId, "type", OWNER);
            if (status.equals(ResponseStatus.ERROR)) {
                event.withMessages(Set.of("error1", "error2"));
                responsesInErrorIds.add(reqId);
            } else if (status.equals(ResponseStatus.SKIPPED)) {
                event.withMessages(Set.of("skipped"));
                responsesInErrorIds.add(reqId);
            } else if (status.equals(ResponseStatus.INVALID_CONTENT)) {
                event.withMessages(Set.of("invalid"));
                responsesInErrorIds.add(reqId);
            }
            event.setHeader(EventHeadersHelper.REQUEST_ID_HEADER, reqId);
            responseEvents.add(event);
        }
    }

}
