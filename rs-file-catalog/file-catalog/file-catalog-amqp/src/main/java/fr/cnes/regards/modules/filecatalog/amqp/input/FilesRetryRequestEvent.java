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
package fr.cnes.regards.modules.filecatalog.amqp.input;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.filecatalog.amqp.output.FileRequestsGroupEvent;
import fr.cnes.regards.modules.filecatalog.dto.FileRequestType;
import fr.cnes.regards.modules.filecatalog.dto.request.FileRetryRequestDto;

import java.util.Collection;

/**
 * Event to to retry error requests.<br/>
 * See {@link FileRequestsGroupEvent} for asynchronous responses when request is finished.<br/>
 * See {@link FileReferenceEvent} for asynchronous responses when a file handled.<br/>
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class FilesRetryRequestEvent extends FileRetryRequestDto implements ISubscribable {

    public FilesRetryRequestEvent() {
        super();
    }

    public FilesRetryRequestEvent(String groupId, Collection<String> owners, FileRequestType type) {
        super(groupId, owners, type);
    }

    public FilesRetryRequestEvent(String groupId, FileRequestType type) {
        super(groupId, type);
    }

    public FilesRetryRequestEvent(Collection<String> owners, FileRequestType type) {
        super(owners, type);
    }

    /**
     * Build a storage retry request for the request business identifier provided
     *
     * @return {@link FilesRetryRequestEvent}
     */
    public static FilesRetryRequestEvent buildStorageRetry(String groupId) {
        return new FilesRetryRequestEvent(groupId, FileRequestType.STORAGE);
    }

    /**
     * Build a storage retry request for all requests in error of provided owners.
     *
     * @return {@link FilesRetryRequestEvent}
     */
    public static FilesRetryRequestEvent buildStorageRetry(Collection<String> owners) {
        return new FilesRetryRequestEvent(owners, FileRequestType.STORAGE);
    }

    /**
     * Build an availability retry request for the given request business identifier
     *
     * @return {@link FilesRetryRequestEvent}
     */
    public static FilesRetryRequestEvent buildAvailabilityRetry(String groupId) {
        return new FilesRetryRequestEvent(groupId, FileRequestType.AVAILABILITY);
    }

}
