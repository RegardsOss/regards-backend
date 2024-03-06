/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.service.flow;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.modules.notifier.dto.in.SpecificRecipientNotificationRequestEvent;
import fr.cnes.regards.modules.notifier.service.NotificationRegistrationService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import java.util.List;

/**
 * Handler to handle {@link SpecificRecipientNotificationRequestEvent} events
 *
 * @author Stephane Cortine
 */
@Component
@Profile("!nohandler")
public class SpecificRecipientNotificationRequestEventHandler
    implements IBatchHandler<SpecificRecipientNotificationRequestEvent>, ApplicationListener<ApplicationReadyEvent> {

    private final ISubscriber subscriber;

    private final NotificationRegistrationService notificationRegistrationService;

    public SpecificRecipientNotificationRequestEventHandler(ISubscriber subscriber,
                                                            NotificationRegistrationService notificationRegistrationService) {
        this.subscriber = subscriber;
        this.notificationRegistrationService = notificationRegistrationService;
    }

    @Override
    public Class<SpecificRecipientNotificationRequestEvent> getMType() {
        return SpecificRecipientNotificationRequestEvent.class;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(SpecificRecipientNotificationRequestEvent.class, this);
    }

    @Override
    public Errors validate(SpecificRecipientNotificationRequestEvent message) {
        return null;
    }

    @Override
    public void handleBatch(List<SpecificRecipientNotificationRequestEvent> messages) {
        notificationRegistrationService.registerNotificationRequests(messages);
    }

    @Override
    public boolean isRetryEnabled() {
        return true;
    }

}
