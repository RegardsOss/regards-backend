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
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.LtaDataType;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.ProductFileDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.submission.reading.SubmissionReadService;
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
import org.springframework.http.MediaType;

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
    private ISubmissionRequestRepository submissionRequestRepository;

    // class under test
    private IngestResponseListener ingestResponseListener;

    @Mock
    private INotifierClient notifierClient;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Spy
    private IPublisher publisher;

    @Mock
    private SubmissionReadService submissionReadService;

    @Before
    public void init() {
        IngestResponseService responseService = new IngestResponseService(submissionRequestRepository,
                                                                          notifierClient,
                                                                          runtimeTenantResolver,
                                                                          new Gson());

        ingestResponseListener = new IngestResponseListener(responseService,
                                                            submissionRequestRepository,
                                                            submissionReadService,
                                                            publisher);
    }

    @Test
    @Purpose("Check that submission requests are successfully updated following the receiving of ingest request events.")
    public void update_request_success() {
        // GIVEN
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
            Mockito.when(submissionRequestRepository.findIdsByCorrelationIdIn(List.of(reqId)))
                   .thenReturn(List.of(reqId));
        }
        SubmissionRequestDto submissionRequestDto = createSubmissionRequestDto();

        // WHEN
        // Responses events are sent to the worker manager listener
        ingestResponseListener.onSuccess(List.of(events.get(0)));
        ingestResponseListener.onGranted(List.of(events.get(1)));
        ingestResponseListener.onDenied(List.of(events.get(2)));
        ingestResponseListener.onError(List.of(events.get(3)));

        //  THEN
        // Check submission requests are updated with correct corresponding status
        Mockito.verify(submissionRequestRepository, times(nbEvents)).updateRequestState(any(), any(), any(), any());

        // success
        Mockito.verify(submissionRequestRepository)
               .updateRequestState(eq(events.get(0).getRequestId()),
                                   eq(SubmissionRequestState.DONE),
                                   eq(null),
                                   any(OffsetDateTime.class));
        // granted
        Mockito.verify(submissionRequestRepository)
               .updateRequestState(eq(events.get(1).getRequestId()),
                                   eq(SubmissionRequestState.INGESTION_PENDING),
                                   eq(null),
                                   any(OffsetDateTime.class));

        // denied
        Mockito.verify(submissionRequestRepository)
               .updateRequestState(eq(events.get(2).getRequestId()),
                                   eq(SubmissionRequestState.INGESTION_ERROR),
                                   eq(StringUtils.join(events.get(2).getErrors(), " | ")),
                                   any(OffsetDateTime.class));
        // error
        Mockito.verify(submissionRequestRepository)
               .updateRequestState(eq(events.get(3).getRequestId()),
                                   eq(SubmissionRequestState.INGESTION_ERROR),
                                   eq(StringUtils.join(events.get(3).getErrors(), " | ")),
                                   any(OffsetDateTime.class));

        //Check messages were sent onSuccess and onError
        ArgumentCaptor<List<ISubscribable>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(publisher, times(2)).publish(argumentCaptor.capture());
        List<SubmissionResponseDtoEvent> capturedSubmissionResponseDtoEvents = getSubmissionResponseDtoEvents(
            argumentCaptor);

        // Check List<SubmissionResponseDtoEvent>
        Assert.assertNotNull(capturedSubmissionResponseDtoEvents);
        Assert.assertEquals("Expected 2 events", 2, capturedSubmissionResponseDtoEvents.size());
        Optional<SubmissionResponseDtoEvent> successEvent = capturedSubmissionResponseDtoEvents.stream()
                                                                                               .filter(event -> event.getResponseStatus()
                                                                                                                     .equals(
                                                                                                                         SubmissionResponseStatus.SUCCESS))
                                                                                               .findFirst();
        //onSuccess
        if (successEvent.isEmpty()) {
            Assert.fail("Expected a SUCCESS event");
        }
        Assert.assertEquals("The event correlation id should match the request id",
                            events.get(0).getRequestId(),
                            successEvent.get().getCorrelationId());

        //onError
        Optional<SubmissionResponseDtoEvent> errorEvent = capturedSubmissionResponseDtoEvents.stream()
                                                                                             .filter(event -> event.getResponseStatus()
                                                                                                                   .equals(
                                                                                                                       SubmissionResponseStatus.ERROR))
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
        // GIVEN
        // Response events init

        // WHEN
        // Responses events are sent to the worker manager listener
        ingestResponseListener.onSuccess(List.of(RequestInfo.build(UUID.randomUUID().toString(), null, null)));

        // THEN
        // Check no submission requests are updated
        Mockito.verify(submissionRequestRepository, times(0)).updateRequestState(any(), any(), any(), any());

        Mockito.verify(publisher, times(1)).publish((List<ISubscribable>) any());
    }

    // ACKs tests
    @Test
    public void test_ackCorrectlySendToNotifierIfRequestSuccess() {
        // GIVEN
        String urnTest = "myUrnTest";
        String tenantName = "tenantName";
        String reqId = UUID.randomUUID().toString();
        RequestInfo event = RequestInfo.build(reqId, reqId, null, null);

        Mockito.when(submissionRequestRepository.findIdsByCorrelationIdIn(List.of(reqId))).thenReturn(List.of(reqId));

        SubmissionRequest request = new SubmissionRequest();
        request.setOriginUrn(urnTest);
        Mockito.when(submissionRequestRepository.findAllByCorrelationIdIn(List.of(event.getRequestId())))
               .thenReturn(List.of(request));

        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn(tenantName);

        // WHEN
        ingestResponseListener.onSuccess(List.of(event));

        // THEN
        ArgumentCaptor<List<NotificationRequestEvent>> eventCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(notifierClient).sendNotifications(eventCaptor.capture());
        List<NotificationRequestEvent> value = eventCaptor.getValue();
        Assert.assertEquals(1, value.size());
        Assert.assertTrue(value.get(0) instanceof NotificationRequestEvent);
        NotificationRequestEvent notif = value.get(0);

        Assert.assertEquals(urnTest, notif.getPayload().get("urn").getAsString());
        Assert.assertEquals(SuccessLtaRequestNotification.NOTIF_ACTION,
                            notif.getMetadata().get("action").getAsString());
        Assert.assertEquals(tenantName, notif.getMetadata().get("sessionOwner").getAsString());
        // Only one notification is sent to notifier.
        Mockito.verify(notifierClient, Mockito.times(1)).sendNotifications(Mockito.any(List.class));

        ArgumentCaptor<List<ISubscribable>> argumentCaptor = ArgumentCaptor.forClass(List.class);

        Mockito.verify(publisher, times(1)).publish(argumentCaptor.capture());
        List<SubmissionResponseDtoEvent> capturedSubmissionResponseDtoEvents = getSubmissionResponseDtoEvents(
            argumentCaptor);

        Assert.assertNotNull(capturedSubmissionResponseDtoEvents);
        Assert.assertEquals("Expected 1 event", 1, capturedSubmissionResponseDtoEvents.size());
    }

    @Test
    public void test_noAckSentIfNotSuccess() {
        // GIVEN
        String reqId = UUID.randomUUID().toString();
        RequestInfo event = RequestInfo.build(reqId, reqId, null, null);
        Mockito.when(submissionRequestRepository.findIdsByCorrelationIdIn(List.of(reqId))).thenReturn(List.of(reqId));

        // WHEN
        ingestResponseListener.onGranted(List.of(event));
        ingestResponseListener.onDenied(List.of(event));
        ingestResponseListener.onError(List.of(event));

        // THEN
        // No success request so zero notif sent.
        Mockito.verify(notifierClient, Mockito.times(0)).sendNotifications(Mockito.any(List.class));
    }

    private SubmissionRequestDto createSubmissionRequestDto() {
        return new SubmissionRequestDto("correlationId",
                                        "id",
                                        "datatype",
                                        IGeometry.unlocated(),
                                        Collections.singletonList(new ProductFileDto(LtaDataType.RAWDATA,
                                                                                     "url",
                                                                                     "filename",
                                                                                     "checksum",
                                                                                     MediaType.APPLICATION_OCTET_STREAM)),
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        false);
    }

    private List<SubmissionResponseDtoEvent> getSubmissionResponseDtoEvents(ArgumentCaptor<List<ISubscribable>> argumentCaptor) {
        return argumentCaptor.getAllValues()
                             .stream()
                             .flatMap(List::stream)
                             .filter(SubmissionResponseDtoEvent.class::isInstance)
                             .map(SubmissionResponseDtoEvent.class::cast)
                             .toList();
    }

}
