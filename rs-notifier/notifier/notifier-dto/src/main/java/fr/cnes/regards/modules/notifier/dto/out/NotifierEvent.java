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
package fr.cnes.regards.modules.notifier.dto.out;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A notification response event after a notification request event
 * ({@link fr.cnes.regards.framework.amqp.event.notifier.NotificationRequestEvent} and {@link fr.cnes.regards.modules.notifier.dto.in.SpecificRecipientNotificationRequestEvent})
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class NotifierEvent implements ISubscribable {

    private final String requestId;

    private final String requestOwner;

    private final NotificationState state;

    private final Set<Recipient> recipients;

    /**
     * Date when notification request has been handled by notifier service.
     * NOTE : As the request can lead to many notifications (one per recipient), this date is not the real
     * notification sent date but the request creation date. For listeners, it is important that this date is at least
     * before the effective sent date for synchronization.
     */
    private final OffsetDateTime notificationDate;

    public NotifierEvent(String requestId,
                         String requestOwner,
                         NotificationState state,
                         OffsetDateTime notificationDate) {
        this(requestId, requestOwner, state, new HashSet<>(), notificationDate);
    }

    public NotifierEvent(String requestId,
                         String requestOwner,
                         NotificationState state,
                         Set<Recipient> recipients,
                         OffsetDateTime notificationDate) {
        this.requestId = requestId;
        this.requestOwner = requestOwner;
        this.state = state;
        this.recipients = recipients;
        this.notificationDate = notificationDate;
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

    public OffsetDateTime getNotificationDate() {
        return notificationDate;
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