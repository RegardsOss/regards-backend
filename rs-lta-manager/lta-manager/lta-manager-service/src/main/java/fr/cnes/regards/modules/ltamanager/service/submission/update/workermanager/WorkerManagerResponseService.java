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
package fr.cnes.regards.modules.ltamanager.service.submission.update.workermanager;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.domain.submission.mapping.WorkerStatusResponseMapping;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Service for {@link WorkerManagerResponseListener}
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class WorkerManagerResponseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerManagerResponseService.class);

    private final ISubmissionRequestRepository requestRepository;

    public WorkerManagerResponseService(ISubmissionRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Update corresponding {@link SubmissionRequest} states following the receiving of worker {@link ResponseEvent}s
     *
     * @param responseEvents events received from worker-manager
     */
    public void updateSubmissionRequestState(List<ResponseEvent> responseEvents) {
        // note: page handling is not necessary because event batch is limited to 1000 entities by default
        List<SubmissionRequestState> allowedStatesToUpdate = List.of(SubmissionRequestState.VALIDATED,
                                                                     SubmissionRequestState.GENERATION_PENDING);
        List<String> requestsFound = requestRepository.findIdsByCorrelationIdInAndStatesIn(responseEvents.stream()
                                                                                                         .map(
                                                                                                         WorkerManagerResponseService::getRequestIdHeader)
                                                                                                         .filter(Objects::nonNull)
                                                                                                         .toList(),
                                                                                           allowedStatesToUpdate);
        LOGGER.trace("{} submission requests found in database among states {}.",
                     requestsFound.size(),
                     allowedStatesToUpdate);

        if (!requestsFound.isEmpty()) {
            for (ResponseEvent event : responseEvents) {
                WorkerStatusResponseMapping mappedRequestState = WorkerStatusResponseMapping.findMappedStatus(event.getState());
                String correlationId = getRequestIdHeader(event);
                if (requestsFound.contains(correlationId) && mappedRequestState != null) {
                    SubmissionRequestState mappedState = mappedRequestState.getMappedState();

                    requestRepository.updateRequestState(correlationId,
                                                         mappedState,
                                                         getRequestMessage(event.getMessage()),
                                                         OffsetDateTime.now());
                    LOGGER.trace("Submission request with correlationId \"{}\" updated with state \"{}\"",
                                 correlationId,
                                 mappedState);
                }
            }
        }
    }

    @Nullable
    private String getRequestMessage(Collection<String> messages) {
        String submissionMessage = null;
        if (!messages.isEmpty()) {
            submissionMessage = StringUtils.join(messages, " | ");
        }
        return submissionMessage;
    }

    private static String getRequestIdHeader(ResponseEvent event) {
        return event.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER);
    }

}
