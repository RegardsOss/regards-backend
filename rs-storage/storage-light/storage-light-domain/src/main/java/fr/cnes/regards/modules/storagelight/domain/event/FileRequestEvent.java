/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storagelight.domain.event;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author sbinda
 *
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FileRequestEvent implements ISubscribable {

    private String requestId;

    private FileRequestEventState state;

    private final Set<ErrorFile> errors = Sets.newHashSet();

    public String getRequestId() {
        return requestId;
    }

    public FileRequestEventState getState() {
        return state;
    }

    public static FileRequestEvent build(String requestId, FileRequestEventState state) {
        FileRequestEvent event = new FileRequestEvent();
        event.requestId = requestId;
        event.state = state;
        return event;
    }

    public static FileRequestEvent buildError(String requestId, Collection<ErrorFile> errors) {
        FileRequestEvent event = new FileRequestEvent();
        event.requestId = requestId;
        event.state = FileRequestEventState.ERROR;
        event.errors.addAll(errors);
        return event;
    }

    public Set<ErrorFile> getErrors() {
        return errors;
    }

    public static class ErrorFile {

        private String checksum;

        private String storage;

        private String errorCause;

        public static ErrorFile build(String checksum, String storage, String errorCause) {
            ErrorFile err = new ErrorFile();
            err.checksum = checksum;
            err.storage = storage;
            err.errorCause = errorCause;
            return err;
        }

        public String getChecksum() {
            return checksum;
        }

        public String getStorage() {
            return storage;
        }

        public String getErrorCause() {
            return errorCause;
        }
    }

}
