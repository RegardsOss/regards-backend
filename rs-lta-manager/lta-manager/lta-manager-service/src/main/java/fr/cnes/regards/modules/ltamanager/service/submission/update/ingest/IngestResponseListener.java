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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.modules.ingest.client.IIngestClientListener;
import fr.cnes.regards.modules.ingest.client.RequestInfo;
import fr.cnes.regards.modules.ltamanager.amqp.output.LtaCleanWorkerRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.domain.submission.mapping.IngestStatusResponseMapping;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.submission.reading.SubmissionReadService;
import fr.cnes.regards.modules.workermanager.amqp.events.EventHeadersHelper;
import fr.cnes.regards.modules.workermanager.amqp.events.in.RequestEvent;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Update {@link fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest} states following the
 * receiving of ingest {@link RequestInfo}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class IngestResponseListener implements IIngestClientListener {

    /**
     * Content type for the LTA Clean worker
     */
    public final static String CONTENT_TYPE_LTA_CLEAN_WORKER = "lta-clean";

    /**
     * Service class for ingest response
     */
    private final IngestResponseService ingestResponseService;

    /**
     * Service class for the reading of submission request
     */
    private final SubmissionReadService submissionReadService;

    private final IPublisher publisher;

    public IngestResponseListener(IngestResponseService responseService,
                                  SubmissionReadService submissionReadService,
                                  IPublisher publisher) {
        this.ingestResponseService = responseService;
        this.submissionReadService = submissionReadService;
        this.publisher = publisher;
    }

    @Override
    public void onDenied(Collection<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.DENIED_MAP);
    }

    @Override
    public void onGranted(Collection<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.GRANTED_MAP);
    }

    @Override
    public void onError(Collection<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.ERROR_MAP);
        List<SubmissionResponseDtoEvent> requestsCompleteError = infos.stream()
                                                                      .map(info -> new SubmissionResponseDtoEvent(info.getRequestId(),
                                                                                                                  SubmissionResponseStatus.DENIED,
                                                                                                                  null,
                                                                                                                  buildErrorMessage(
                                                                                                                      info.getErrors())))
                                                                      .toList();
        publisher.publish(requestsCompleteError);

    }

    /**
     * Build the error message
     *
     * @param errors the set of errors
     * @return the error message
     */
    private String buildErrorMessage(Set<String> errors) {
        if (errors == null) {
            return null;
        }
        StringBuilder errorMessage = new StringBuilder();
        for (String error : errors) {
            errorMessage.append(error);
            errorMessage.append("  \\n");
        }
        return errorMessage.toString();
    }

    @Override
    public void onSuccess(Collection<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.SUCCESS_MAP);

        List<SubmissionResponseDtoEvent> submissionResponseDtoEvents = new ArrayList<>();
        List<LtaCleanWorkerRequestDtoEvent> ltaCleanWorkerRequestDtoEvents = new ArrayList<>();
        infos.stream().forEach(info -> {
            submissionResponseDtoEvents.add(new SubmissionResponseDtoEvent(info.getRequestId(),
                                                                           SubmissionResponseStatus.GRANTED,
                                                                           null,
                                                                           null));

            Optional<SubmissionRequestDto> submissionRequestDto = submissionReadService.findSubmissionRequestByCorrelationId(
                info.getRequestId());
            if (submissionRequestDto.isPresent()) {
                LtaCleanWorkerRequestDtoEvent ltaCleanWorkerRequestDtoEvent = new LtaCleanWorkerRequestDtoEvent(
                    submissionRequestDto.get().getCorrelationId(),
                    submissionRequestDto.get().getId(),
                    submissionRequestDto.get().getDatatype(),
                    submissionRequestDto.get().getGeometry(),
                    submissionRequestDto.get().getFiles());
                ltaCleanWorkerRequestDtoEvent.addHeader(EventHeadersHelper.CONTENT_TYPE_HEADER,
                                                        CONTENT_TYPE_LTA_CLEAN_WORKER);

                ltaCleanWorkerRequestDtoEvents.add(ltaCleanWorkerRequestDtoEvent);
            }
        });
        publisher.publish(submissionResponseDtoEvents);

        // Publish list of events to LTA clean worker
        publisher.publish(ltaCleanWorkerRequestDtoEvents,
                          "regards.broadcast." + RequestEvent.class.getName(),
                          Optional.empty());
    }

    @Override
    public void onDeleted(Set<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.DELETED_MAP);
    }
}
