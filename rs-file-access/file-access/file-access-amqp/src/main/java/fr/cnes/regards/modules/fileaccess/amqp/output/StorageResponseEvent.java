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
package fr.cnes.regards.modules.fileaccess.amqp.output;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseDto;

/**
 * Response of a {@link FileStorageRequestReadyToProcessEvent}
 *
 * @author Thibaud Michaudel
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class StorageResponseEvent extends StorageResponseDto implements ISubscribable {

    private StorageResponseEvent(Long requestId,
                                 String url,
                                 String checksum,
                                 int size,
                                 int height,
                                 int weight,
                                 boolean storedInCache,
                                 String errorType,
                                 String error) {
        super(requestId, url, checksum, size, height, weight, storedInCache, errorType, error);
    }

    private StorageResponseEvent(Long requestId,
                                 String url,
                                 String checksum,
                                 int size,
                                 int height,
                                 int weight,
                                 boolean storedInCache) {
        super(requestId, url, checksum, size, height, weight, storedInCache);
    }

    private StorageResponseEvent(Long requestId, String url, String checksum, String errorType, String error) {
        super(requestId, url, checksum, errorType, error);
    }

    /**
     * Success event
     */
    public static StorageResponseEvent createSuccessResponse(Long requestId,
                                                             String url,
                                                             String checksum,
                                                             int size,
                                                             int height,
                                                             int weight,
                                                             boolean storedInCache) {
        return new StorageResponseEvent(requestId, url, checksum, size, height, weight, storedInCache);
    }

    /**
     * Error event
     */
    public static StorageResponseEvent createErrorResponse(Long requestId,
                                                           String url,
                                                           String checksum,
                                                           String errorType,
                                                           String error) {
        return new StorageResponseEvent(requestId, url, checksum, errorType, error);
    }
}
