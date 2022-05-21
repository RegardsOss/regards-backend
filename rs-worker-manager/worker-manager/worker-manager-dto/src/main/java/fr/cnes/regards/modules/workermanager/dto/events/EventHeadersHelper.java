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
public final class EventHeadersHelper {

    public static final String TENANT_HEADER = "regards.tenant";

    public static final String REQUEST_ID_HEADER = "regards.request.id";

    public static final String OWNER_HEADER = "regards.request.owner";

    public static final String SESSION_HEADER = "regards.request.session";

    public static final String CONTENT_TYPE_HEADER = "regards.request.content_type";

    public static final String DLQ_ERROR_STACKTRACE_HEADER = "x-exception-stacktrace";

    public static final String WORKER_ID = "regards.worker_id";

    public static final String MISSING_HEADER_CODE = "MISSING_HEADER";

    private EventHeadersHelper() {
    }

    public static Optional<String> getOwnerHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeadersHelper.OWNER_HEADER));
    }

    public static Optional<String> getRequestIdHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER));
    }

    public static Optional<String> getContentTypeHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeadersHelper.CONTENT_TYPE_HEADER));
    }

    public static Optional<String> getTenantHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeadersHelper.TENANT_HEADER));
    }

    public static Optional<String> getSessionHeader(Message message) {
        return Optional.ofNullable(message.getMessageProperties().getHeader(EventHeadersHelper.SESSION_HEADER));
    }

    public static Errors validateHeader(IMessagePropertiesAware message) {
        DataBinder db = new DataBinder(message);
        String requestId = message.getMessageProperties().getHeader(EventHeadersHelper.REQUEST_ID_HEADER);
        Errors errors = db.getBindingResult();
        if (StringUtils.isEmpty(requestId)) {
            errors.rejectValue(EventHeadersHelper.REQUEST_ID_HEADER, EventHeadersHelper.MISSING_HEADER_CODE);
        }
        return errors;
    }
}
