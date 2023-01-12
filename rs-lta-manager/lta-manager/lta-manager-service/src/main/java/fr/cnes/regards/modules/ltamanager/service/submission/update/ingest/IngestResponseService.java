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
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.mapping.IngestStatusResponseMapping;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.service.submission.update.ingest.notification.SuccessLtaRequestNotification;
import fr.cnes.regards.modules.notifier.client.INotifierClient;
import fr.cnes.regards.modules.notifier.dto.in.NotificationRequestEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Service for {@link IngestResponseListener}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class IngestResponseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestResponseService.class);

    private final ISubmissionRequestRepository requestRepository;

    private final INotifierClient notifierClient;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final Gson gson;

    public IngestResponseService(ISubmissionRequestRepository requestRepository,
                                 INotifierClient notifierClient,
                                 IRuntimeTenantResolver runtimeTenantResolver,
                                 Gson gson) {
        this.requestRepository = requestRepository;
        this.notifierClient = notifierClient;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.gson = gson;
    }

    /**
     * Update corresponding {@link SubmissionRequest} states following the receiving of {@link RequestInfo}s
     *
     * @param responseEvents     events received from ingest
     * @param mappedRequestState corresponding state to be updated
     */
    public void updateSubmissionRequestState(Collection<RequestInfo> responseEvents,
                                             IngestStatusResponseMapping mappedRequestState) {
        // note: page handling is not necessary because event batch is limited to 1000 entities by default
        List<String> requestsFound = requestRepository.findIdsByCorrelationIdIn(responseEvents.stream()
                                                                                              .map(RequestInfo::getRequestId)
                                                                                              .toList());
        LOGGER.trace("{} submission requests found in database.", requestsFound.size());

        List<RequestInfo> requestsUpdated = new ArrayList<>();
        if (!requestsFound.isEmpty()) {
            for (RequestInfo event : responseEvents) {
                String correlationId = event.getRequestId();
                if (requestsFound.contains(correlationId)) {
                    SubmissionRequestState mappedState = mappedRequestState.getMappedState();

                    requestRepository.updateRequestState(correlationId,
                                                         mappedState,
                                                         getRequestMessage(event.getErrors(), mappedRequestState),
                                                         OffsetDateTime.now());
                    requestsUpdated.add(event);
                    LOGGER.trace("Submission request with correlationId \"{}\" updated with state \"{}\"",
                                 correlationId,
                                 mappedState);
                }
            }
        }
        sendNotifToOtherCatalogIfNeeded(requestsUpdated, mappedRequestState);
    }

    private void sendNotifToOtherCatalogIfNeeded(List<RequestInfo> events,
                                                 IngestStatusResponseMapping mappedRequestState) {
        if (mappedRequestState != IngestStatusResponseMapping.SUCCESS_MAP) {
            // do nothing if not success
            return;
        }
        List<SubmissionRequest> requests = requestRepository.findAllByCorrelationIdIn(events.stream()
                                                                                            .map(RequestInfo::getRequestId)
                                                                                            .toList());
        String currentTenant = runtimeTenantResolver.getTenant();
        List<NotificationRequestEvent> notifsToSend = requests.stream()
                                                              // do nothing if no origin urn set
                                                              .filter(request -> request.getOriginUrn() != null)
                                                              .map(request -> SuccessLtaRequestNotification.fromRequest(
                                                                  request,
                                                                  currentTenant,
                                                                  gson))
                                                              .map(NotificationRequestEvent.class::cast)
                                                              .toList();
        if (!notifsToSend.isEmpty()) {
            notifierClient.sendNotifications(notifsToSend);
        }
    }

    @Nullable
    private String getRequestMessage(@Nullable Set<String> errors, IngestStatusResponseMapping mapping) {
        String submissionMessage = null;
        if (mapping == IngestStatusResponseMapping.DELETED_MAP) {
            submissionMessage = "The submission request has been cancelled by ingest.";
        }
        if (errors != null && !errors.isEmpty()) {
            submissionMessage = StringUtils.join(errors, " | ");
        }
        return submissionMessage;
    }

}
