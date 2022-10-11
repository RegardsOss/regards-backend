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

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Before
    public void init() {
        IngestResponseService responseService = new IngestResponseService(requestRepository);
        responseListener = new IngestResponseListener(responseService);
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

}
