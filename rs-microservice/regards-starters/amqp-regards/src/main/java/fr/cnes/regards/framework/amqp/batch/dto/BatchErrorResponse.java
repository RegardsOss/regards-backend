/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.batch.dto;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.notification.NotificationEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Object to collect errors during the processing of a batch of AMQP messages. It contains notification errors and
 * response messages provided optionally.
 *
 * @author Iliana Ghazali
 **/
public final class BatchErrorResponse {

    private final List<NotificationEvent> notificationErrors;

    private final List<ResponseMessage<? extends ISubscribable>> responseMessages;

    public BatchErrorResponse() {
        this.notificationErrors = new ArrayList<>();
        this.responseMessages = new ArrayList<>();
    }

    public void appendError(NotificationEvent notificationError,
                            ResponseMessage<? extends ISubscribable> responseMessage) {
        this.notificationErrors.add(notificationError);
        if (responseMessage.hasPayload()) {
            this.responseMessages.add(responseMessage);
        }
    }

    public boolean hasErrors() {
        return !notificationErrors.isEmpty();
    }

    public List<ResponseMessage<? extends ISubscribable>> getResponseMessages() {
        return responseMessages;
    }

    public List<NotificationEvent> getNotificationErrors() {
        return notificationErrors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BatchErrorResponse that = (BatchErrorResponse) o;
        return Objects.equals(notificationErrors, that.notificationErrors) && Objects.equals(responseMessages,
                                                                                             that.responseMessages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(notificationErrors, responseMessages);
    }

    @Override
    public String toString() {
        return "BatchErrorResponse{"
               + "notificationErrors="
               + notificationErrors
               + ", responseMessages="
               + responseMessages
               + '}';
    }
}
