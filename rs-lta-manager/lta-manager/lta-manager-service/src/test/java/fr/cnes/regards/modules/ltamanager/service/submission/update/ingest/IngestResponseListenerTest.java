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
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.ltamanager.service.submission.update.ingest;

import com.google.gson.Gson;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaRequestCompleteEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.LtaRequestCompleteState;
import fr.cnes.regards.modules.ltamanager.service.submission.update.ingest.notification.SuccessLtaRequestNotification;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

/**
 * Test for {@link IngestResponseListener}
 *
 * @author Iliana Ghazali
 **/
@RunWith(MockitoJUnitRunner.class)
public class IngestResponseListenerTest {

    @Mock
    private ISubmissionRequestRepository requestRepository;

    // class under test
    private IngestResponseListener responseListener;

    @Mock
    private INotifierClient notifierClient;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Spy
    private IPublisher publisher;

    @Before
    public void init() {
        IngestResponseService responseService = new IngestResponseService(requestRepository,
                                                                          notifierClient,
                                                                          runtimeTenantResolver,
                                                                          new Gson());
        responseListener = new IngestResponseListener(responseService, publisher);
    }

    @Test
    @Purpose(
        "Check that submission requests are successfully updated following the receiving of ingest request events.")
    public void update_request_success() {
        // ---- GIVEN ----
        // Create request info event
        int nbEvents = 4;
        List<RequestInfo> events = new ArrayList<>();
        for (int i = 0; i < nbEvents; i++) {
            String reqId = UUID.randomUUID().toString();
            RequestInfo event = RequestInfo.build(reqId, reqId, null, null);
            if (i == 2) {
                event.setErrors(Set.of("denied"));
            } else if (i == 3) {
                event.setErrors(Set.of("error1", "error2"));
            }
            events.add(event);
            // Mock database behaviour to simulate request ids exist
            Mockito.when(requestRepository.findIdsByRequestIdIn(List.of(reqId))).thenReturn(List.of(reqId));
        }

        // ---- WHEN -----
        // Responses events are sent to the worker manager listener
        responseListener.onSuccess(List.of(events.get(0)));
        responseListener.onGranted(List.of(events.get(1)));
        responseListener.onDenied(List.of(events.get(2)));
        responseListener.onError(List.of(events.get(3)));

        // ---- THEN -----
        // Check submission requests are updated with correct corresponding status
        Mockito.verify(requestRepository, times(nbEvents)).updateRequestState(any(), any(), any(), any());

        // success
        Mockito.verify(requestRepository)
               .updateRequestState(eq(events.get(0).getRequestId()),
                                   eq(SubmissionRequestState.DONE),
                                   eq(null),
                                   any(OffsetDateTime.class));
        // granted
        Mockito.verify(requestRepository)
               .updateRequestState(eq(events.get(1).getRequestId()),
                                   eq(SubmissionRequestState.INGESTION_PENDING),
                                   eq(null),
                                   any(OffsetDateTime.class));

        // denied
        Mockito.verify(requestRepository)
               .updateRequestState(eq(events.get(2).getRequestId()),
                                   eq(SubmissionRequestState.INGESTION_ERROR),
                                   eq(StringUtils.join(events.get(2).getErrors(), " | ")),
                                   any(OffsetDateTime.class));
        // error
        Mockito.verify(requestRepository)
               .updateRequestState(eq(events.get(3).getRequestId()),
                                   eq(SubmissionRequestState.INGESTION_ERROR),
                                   eq(StringUtils.join(events.get(3).getErrors(), " | ")),
                                   any(OffsetDateTime.class));

        //Check messages were sent onSuccess and onError
        ArgumentCaptor<List<LtaRequestCompleteEvent>> captorPublished = ArgumentCaptor.forClass(List.class);
        Mockito.verify(publisher, times(2)).publish(captorPublished.capture());
        List<LtaRequestCompleteEvent> capturedPublishedEvents = captorPublished.getAllValues()
                                                                               .stream()
                                                                               .flatMap(List::stream)
                                                                               .toList();
        Assert.assertEquals("Expected 2 events", 2, capturedPublishedEvents.size());
        Optional<LtaRequestCompleteEvent> successEvent = capturedPublishedEvents.stream()
                                                                                .filter(event -> event.getStatus()
                                                                                                      .equals(
                                                                                                          LtaRequestCompleteState.SUCCESS))
                                                                                .findFirst();
        //onSuccess
        if (successEvent.isEmpty()) {
            Assert.fail("Expected a SUCCESS event");
        }
        Assert.assertEquals("The event correlation id should match the request id",
                            events.get(0).getRequestId(),
                            successEvent.get().getCorrelationId());

        //onError
        Optional<LtaRequestCompleteEvent> errorEvent = capturedPublishedEvents.stream()
                                                                              .filter(event -> event.getStatus()
                                                                                                    .equals(
                                                                                                        LtaRequestCompleteState.ERROR))
                                                                              .findFirst();
        if (errorEvent.isEmpty()) {
            Assert.fail("Expected an ERROR event");
        }
        Assert.assertEquals("The event correlation id should match the request id",
                            events.get(3).getRequestId(),
                            errorEvent.get().getCorrelationId());

    }

    @Test
    @Purpose("Check that no submission requests are updated if request events received do not correspond to any "
             + "request")
    public void update_request_no_request_update() {
        // ---- GIVEN ----
        // Response events init

        // ---- WHEN ----
        // Responses events are sent to the worker manager listener
        responseListener.onSuccess(List.of(RequestInfo.build(UUID.randomUUID().toString(), null, null)));

        // --- THEN ----
        // Check no submission requests are updated
        Mockito.verify(requestRepository, times(0)).updateRequestState(any(), any(), any(), any());
    }

    // ACKs tests
    @Test
    public void test_ackCorrectlySendToNotifierIfRequestSuccess() {
        // GIVEN
        String urnTest = "myUrnTest";
        String tenantName = "tenantName";
        String reqId = UUID.randomUUID().toString();
        RequestInfo event = RequestInfo.build(reqId, reqId, null, null);
        Mockito.when(requestRepository.findIdsByRequestIdIn(List.of(reqId))).thenReturn(List.of(reqId));
        SubmissionRequest request = new SubmissionRequest();
        request.setOriginUrn(urnTest);
        Mockito.when(requestRepository.findAllByRequestIdIn(List.of(event.getRequestId())))
               .thenReturn(List.of(request));
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(tenantName);
        // WHEN
        responseListener.onSuccess(List.of(event));
        // THEN
        ArgumentCaptor<List<NotificationRequestEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(notifierClient).sendNotifications(eventCaptor.capture());
        List<NotificationRequestEvent> value = eventCaptor.getValue();
        Assert.assertEquals(1, value.size());
        NotificationRequestEvent notif = value.get(0);
        Assert.assertTrue(notif instanceof NotificationRequestEvent);
        Assert.assertEquals(urnTest, notif.getPayload().get("urn").getAsString());
        Assert.assertEquals(SuccessLtaRequestNotification.NOTIF_ACTION,
                            notif.getMetadata().get("action").getAsString());
        Assert.assertEquals(tenantName, notif.getMetadata().get("sessionOwner").getAsString());
        // Only one notification is sent to notifier.
        Mockito.verify(notifierClient, Mockito.times(1)).sendNotifications(Mockito.any(List.class));
    }

    @Test
    public void test_noAckSentIfNotSuccess() {
        // GIVEN
        String reqId = UUID.randomUUID().toString();
        RequestInfo event = RequestInfo.build(reqId, reqId, null, null);
        Mockito.when(requestRepository.findIdsByRequestIdIn(List.of(reqId))).thenReturn(List.of(reqId));
        // WHEN
        responseListener.onGranted(List.of(event));
        responseListener.onDenied(List.of(event));
        responseListener.onError(List.of(event));
        // THEN
        // No success request so zero notif sent.
        Mockito.verify(notifierClient, Mockito.times(0)).sendNotifications(Mockito.any(List.class));
    }

}
