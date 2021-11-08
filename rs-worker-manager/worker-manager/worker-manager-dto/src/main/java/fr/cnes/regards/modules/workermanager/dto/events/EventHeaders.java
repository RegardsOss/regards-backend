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
package fr.cnes.regards.modules.workermanager.dto.events;

import fr.cnes.regards.framework.amqp.event.IMessagePropertiesAware;
import org.springframework.amqp.core.Message;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;

import java.util.Optional;

/**
 * List all possible values for WorkerManger requests
 *
 * @author Sébastien Binda
 */
public enum EventHeaders {

    TENANT_HEADER("regards.tenant"),

    REQUEST_ID_HEADER("regards.request.id"),

    OWNER_HEADER("regards.request.owner"),

    SESSION_HEADER("regards.request.session"),

    CONTENT_TYPE_HEADER("regards.request.content_type"),

    DLQ_ERROR_STACKTRACE_HEADER("x-exception-stacktrace"),

    WORKER_ID("regards.worker_id");

    private final String name;

    public static final String MISSING_HEADER_CODE= "MISSING_HEADER";

    EventHeaders(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static Optional<String> getOwnerHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeaders.OWNER_HEADER.getName()));
    }

    public static Optional<String> getRequestIdHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeaders.REQUEST_ID_HEADER.getName()));
    }

    public static Optional<String> getContentTypeHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeaders.CONTENT_TYPE_HEADER.getName()));
    }

    public static Optional<String> getTenantHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeaders.TENANT_HEADER.getName()));
    }

    public static Optional<String> getSessionHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeaders.SESSION_HEADER.getName()));
    }

    public static Errors validateHeader(IMessagePropertiesAware message) {
        DataBinder db = new DataBinder(message);
        String requestId = message.getMessageProperties().getHeader(EventHeaders.REQUEST_ID_HEADER.getName());
        Errors errors = db.getBindingResult();
        if (StringUtils.isEmpty(requestId)) {
            errors.rejectValue(EventHeaders.REQUEST_ID_HEADER.getName(), EventHeaders.MISSING_HEADER_CODE);
        }
        return errors;
    }
}
