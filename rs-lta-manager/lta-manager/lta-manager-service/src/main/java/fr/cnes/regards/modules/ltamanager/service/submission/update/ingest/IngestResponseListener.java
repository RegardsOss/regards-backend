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
import fr.cnes.regards.modules.ltamanager.domain.submission.mapping.IngestStatusResponseMapping;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import org.springframework.stereotype.Service;

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

    private final IngestResponseService responseService;

    private final IPublisher publisher;

    public IngestResponseListener(IngestResponseService responseService, IPublisher publisher) {
        this.responseService = responseService;
        this.publisher = publisher;
    }

    @Override
    public void onDenied(Collection<RequestInfo> infos) {
        responseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.DENIED_MAP);
    }

    @Override
    public void onGranted(Collection<RequestInfo> infos) {
        responseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.GRANTED_MAP);
    }

    @Override
    public void onError(Collection<RequestInfo> infos) {
        responseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.ERROR_MAP);
        List<SubmissionResponseDtoEvent> requestsCompleteError = infos.stream()
                                                                      .map(info -> new SubmissionResponseDtoEvent(info.getRequestId(),
                                                                                                                  SubmissionResponseStatus.DENIED,
                                                                                                                  buildErrorMessage(
                                                                                                                      info.getErrors())))
                                                                      .toList();
        publisher.publish(requestsCompleteError);

    }

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
        responseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.SUCCESS_MAP);
        List<SubmissionResponseDtoEvent> requestsCompleteSuccess = infos.stream()
                                                                        .map(info -> new SubmissionResponseDtoEvent(info.getRequestId(),
                                                                                                                    SubmissionResponseStatus.GRANTED,
                                                                                                                    null))
                                                                        .toList();
        publisher.publish(requestsCompleteSuccess);
    }

    @Override
    public void onDeleted(Set<RequestInfo> infos) {
        responseService.updateSubmissionRequestState(infos, IngestStatusResponseMapping.DELETED_MAP);
    }
}
