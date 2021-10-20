/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.notifier.dto.out;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class NotifierEvent implements ISubscribable {

    private final String requestId;
    private final String requestOwner;
    private final NotificationState state;
    private final Set<Recipient> recipients;

    public NotifierEvent(String requestId, String requestOwner, NotificationState state) {
        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.state = state;
        this.recipients = new HashSet<>();
    }

    public NotifierEvent(String requestId, String requestOwner, NotificationState state, Set<Recipient> recipients) {
        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.state = state;
        this.recipients = recipients;
    }

    public String getRequestId() {
        return requestId;
    }

    public NotificationState getState() {
        return state;
    }

    public String getRequestOwner() {
        return requestOwner;
    }

    public Set<Recipient> getRecipients() {
        return recipients;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NotifierEvent that = (NotifierEvent) o;
        return Objects.equals(requestId, that.requestId) && Objects.equals(requestOwner, that.requestOwner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, requestOwner);
    }

}