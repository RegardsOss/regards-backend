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
package fr.cnes.regards.modules.notifier.conf.plugin;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.notifier.domain.NotificationRequest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Léo Mieulet
 */
@Plugin(author = "REGARDS Team", description = "Recipient sender", id = RecipientSender.PLUGIN_ID, version = "1.0.0",
    contact = "regards@c-s.fr", license = "GPLv3", owner = "CNES", url = "https://regardsoss.github.io/")
public class RecipientSender implements fr.cnes.regards.modules.notifier.domain.plugin.IRecipientNotifier,
    IHandler<RecipientSender.NotificationEvent> {

    public static final String PLUGIN_ID = "RecipientSender";

    @Autowired
    IPublisher publisher;

    @Override
    public List send(Collection<NotificationRequest> requestsToSend) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getRecipientLabel() {
        return "recipientLabel";
    }

    @Override
    public boolean isAckRequired() {
        return false;
    }

    @Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
    public class NotificationEvent implements ISubscribable {

    }
}
