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

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseStatus;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseEvent;
import fr.cnes.regards.modules.workermanager.amqp.events.out.ResponseStatus;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;

/**
 * Update {@link fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest} states following the
 * receiving of worker {@link ResponseEvent}s.
 *
 * @author Iliana Ghazali
 **/
@Service
public class WorkerManagerResponseListener
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<ResponseEvent> {

    private final WorkerManagerResponseService responseService;

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    private final Validator validator;

    public WorkerManagerResponseListener(WorkerManagerResponseService responseService,
                                         ISubscriber subscriber,
                                         IPublisher publisher,
                                         Validator validator) {
        this.responseService = responseService;
        this.subscriber = subscriber;
        this.publisher = publisher;
        this.validator = validator;
    }

    @Override
    public Errors validate(ResponseEvent responseEvent) {
        Errors errors = new MapBindingResult(new HashMap<>(), responseEvent.getClass().getName());
        validator.validate(responseEvent, errors);
        return errors;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(ResponseEvent.class, this);
    }

    @Override
    public void handleBatch(List<ResponseEvent> responseEvents) {
        LOGGER.trace("[LTA WORKER RESPONSE EVENT HANDLER] Handling {} RequestEvents...", responseEvents.size());
        long start = System.currentTimeMillis();
        responseService.updateSubmissionRequestState(responseEvents);

        Set<ResponseStatus> finalStatus = EnumSet.of(ResponseStatus.ERROR,
                                                     ResponseStatus.SKIPPED,
                                                     ResponseStatus.INVALID_CONTENT);
        List<SubmissionResponseDtoEvent> requestsCompleteError = responseEvents.stream()
                                                                               .filter(response -> finalStatus.contains(
                                                                                   response.getState()))
                                                                               .map(response -> new SubmissionResponseDtoEvent(
                                                                                   response.getRequestId(),
                                                                                   SubmissionResponseStatus.DENIED,
                                                                                   null,
                                                                                   buildErrorMessage(response.getMessage())))
                                                                               .toList();
        if (!requestsCompleteError.isEmpty()) {
            publisher.publish(requestsCompleteError);
        }
        LOGGER.trace("[LTA WORKER RESPONSE EVENT HANDLER] {} RequestEvents handled in {} ms...",
                     responseEvents.size(),
                     System.currentTimeMillis() - start);
    }

    private String buildErrorMessage(Collection<String> errors) {
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

}
