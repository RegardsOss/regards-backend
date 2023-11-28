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
package fr.cnes.regards.modules.storage.service.file.flow;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.filecatalog.amqp.input.CancelRequestEvent;
import fr.cnes.regards.modules.storage.service.file.request.RequestsGroupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import java.util.List;

/**
 * Handler to listen for {@link CancelRequestEvent} amqp events.
 * Those events allow to cancel all requests associated to given group ids.
 *
 * @author Sébastien Binda
 **/
@Component
public class CancelRequestGroupsEventHandler
    implements ApplicationListener<ApplicationReadyEvent>, IBatchHandler<CancelRequestEvent> {

    @Value("${regards.storage.cancel.items.bulk.size:100}")
    private final int BULK_SIZE = 100;

    private RequestsGroupService requestsGroupService;

    private ISubscriber subscriber;

    private Validator validator;

    public CancelRequestGroupsEventHandler(ISubscriber subscriber,
                                           RequestsGroupService requestsGroupService,
                                           Validator validator) {
        this.subscriber = subscriber;
        this.requestsGroupService = requestsGroupService;
        this.validator = validator;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(CancelRequestEvent.class, this);
    }

    @Override
    public int getBatchSize() {
        return BULK_SIZE;
    }

    @Override
    public Errors validate(CancelRequestEvent message) {
        DataBinder dataBinder = new DataBinder(message);
        Errors errors = dataBinder.getBindingResult();
        validator.validate(message, errors);
        return errors;
    }

    @Override
    public void handleBatch(List<CancelRequestEvent> messages) {
        messages.stream()
                .flatMap(message -> message.getGroupsToCancel().stream())
                .forEach(requestsGroupService::cancelRequestGroup);
    }
}
