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
package fr.cnes.regards.modules.ltamanager.service.submission.creation.amqp;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.ltamanager.amqp.input.SubmissionRequestDtoEvent;
import fr.cnes.regards.modules.ltamanager.amqp.output.SubmissionResponseDtoEvent;
import fr.cnes.regards.modules.ltamanager.domain.submission.SubmissionRequest;
import fr.cnes.regards.modules.ltamanager.dto.submission.output.SubmissionResponseDto;
import fr.cnes.regards.modules.ltamanager.service.submission.creation.SubmissionCreateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.HashMap;
import java.util.List;

/**
 * AMQP Message handler to create and save {@link SubmissionRequest}s from {@link SubmissionRequestDtoEvent}s
 */
@Component
public class SubmissionCreateEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<SubmissionRequestDtoEvent> {

    private final SubmissionCreateService createService;

    private final ISubscriber subscriber;

    private final IPublisher publisher;

    private final Validator validator;

    /**
     * Bulk size limit to handle messages
     */
    @Value("${regards.ltamanager.request.bulk.size:1000}")
    private int bulkSize;

    public SubmissionCreateEventHandler(ISubscriber subscriber,
                                        SubmissionCreateService createService,
                                        IPublisher publisher,
                                        Validator validator) {
        this.subscriber = subscriber;
        this.createService = createService;
        this.publisher = publisher;
        this.validator = validator;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SubmissionRequestDtoEvent.class, this);
    }

    @Override
    public int getBatchSize() {
        return bulkSize;
    }

    @Override
    public Class<SubmissionRequestDtoEvent> getMType() {
        return SubmissionRequestDtoEvent.class;
    }

    @Override
    public void handleBatch(List<SubmissionRequestDtoEvent> events) {
        long start = System.currentTimeMillis();
        LOGGER.debug("Handling {} submission request events", events.size());

        // save submission requests to database and publish responses
        List<SubmissionResponseDto> responses = createService.handleSubmissionRequestsCreation(events);
        publisher.publish(responses.stream().map(SubmissionResponseDtoEvent::new).toList());

        LOGGER.debug("{} submission responses created from {} submission request events. Handled in {}ms.",
                     responses.size(),
                     events.size(),
                     System.currentTimeMillis() - start);
    }

    @Override
    public Errors validate(SubmissionRequestDtoEvent requestDto) {
        Errors errors = new MapBindingResult(new HashMap<>(), requestDto.getClass().getName());
        validator.validate(requestDto, errors);
        return errors;
    }

    @Override
    public boolean isDedicatedDLQEnabled() {
        return true;
    }
}
