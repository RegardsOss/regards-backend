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
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.submission.mapping.IngestStatusResponseMapping;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.ltamanager.service.submission.reading.SubmissionReadService;
import fr.cnes.regards.modules.ltamanager.service.utils.SubmissionResponseDtoUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Update {@link fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest} states following the
 * receiving of ingest {@link RequestInfo}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class IngestResponseListener implements IIngestClientListener {

    /**
     * Service class for ingest response
     */
    private final IngestResponseService ingestResponseService;

    /**
     * Service class for the reading of submission request
     */
    private final SubmissionReadService submissionReadService;

    private final IPublisher publisher;

    private final ISubmissionRequestRepository requestRepository;

    public IngestResponseListener(IngestResponseService responseService,
                                  ISubmissionRequestRepository requestRepository,
                                  SubmissionReadService submissionReadService,
                                  IPublisher publisher) {
        this.ingestResponseService = responseService;
        this.submissionReadService = submissionReadService;
        this.publisher = publisher;
        this.requestRepository = requestRepository;
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
                                                                      .map(info -> createSubmissionResponseDtoEvent(info,
                                                                                                                    SubmissionResponseStatus.ERROR,
                                                                                                                    SubmissionResponseDtoUtils.buildErrorMessage(
                                                                                                                        info.getErrors())))
                                                                      .toList();
        publisher.publish(requestsCompleteError);

    }

    @Override
    public void onSuccess(Collection<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.SUCCESS_MAP);

        List<SubmissionResponseDtoEvent> submissionResponseDtoEvents = infos.stream()
                                                                            .map(info -> createSubmissionResponseDtoEvent(
                                                                                info,
                                                                                SubmissionResponseStatus.SUCCESS,
                                                                                null))
                                                                            .toList();
        publisher.publish(submissionResponseDtoEvents);
    }

    /**
     * Build a {@link SubmissionResponseDtoEvent}
     */
    private SubmissionResponseDtoEvent createSubmissionResponseDtoEvent(RequestInfo info,
                                                                        SubmissionResponseStatus status,
                                                                        @Nullable String errorMessage) {
        return SubmissionResponseDtoUtils.createEvent(info.getRequestId(),
                                                      requestRepository.findSubmissionRequestByCorrelationId(info.getRequestId()),
                                                      status,
                                                      errorMessage,
                                                      null,
                                                      1);
    }

    @Override
    public void onDeleted(Set<RequestInfo> infos) {
        ingestResponseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.DELETED_MAP);
    }
}
