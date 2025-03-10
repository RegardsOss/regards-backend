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

import com.fasterxml.jackson.annotation.JsonCreator;
import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseDto;
import fr.cnes.regards.modules.fileaccess.dto.output.StorageResponseErrorEnum;

/**
 * Response of a {@link FileStorageRequestReadyToProcessEvent}
 *
 * @author Thibaud Michaudel
 **/
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE)
public class StorageResponseEvent extends StorageResponseDto implements ISubscribable {

    /**
     * Full constructor for jackson
     */
    @JsonCreator
    public StorageResponseEvent(String requestId,
                                String url,
                                String checksum,
                                long size,
                                Integer height,
                                Integer width,
                                boolean storedInCache,
                                String finalArchiveParentUrl,
                                String fileCachePath,
                                StorageResponseErrorEnum errorType,
                                String error) {
        super(requestId,
              url,
              checksum,
              size,
              height,
              width,
              storedInCache,
              finalArchiveParentUrl,
              fileCachePath,
              errorType,
              error);
    }

    private StorageResponseEvent(String requestId,
                                 String url,
                                 String checksum,
                                 long size,
                                 Integer height,
                                 Integer width) {
        super(requestId, url, checksum, size, height, width);
    }

    private StorageResponseEvent(String requestId,
                                 String url,
                                 String checksum,
                                 long size,
                                 Integer height,
                                 Integer width,
                                 String finalArchiveParentUrl,
                                 String fileCachePath) {
        super(requestId, url, checksum, size, height, width, finalArchiveParentUrl, fileCachePath);
    }

    private StorageResponseEvent(String requestId, String url, String checksum) {
        super(requestId, url, checksum);
    }

    private StorageResponseEvent(String requestId,
                                 String url,
                                 String checksum,
                                 StorageResponseErrorEnum errorType,
                                 String error) {
        super(requestId, url, checksum, errorType, error);
    }

    /**
     * Success event
     */
    public static StorageResponseEvent createSuccessResponse(String requestId,
                                                             String url,
                                                             String checksum,
                                                             long size,
                                                             Integer height,
                                                             Integer width) {
        return new StorageResponseEvent(requestId, url, checksum, size, height, width);
    }

    /**
     * Success cache event
     */
    public static StorageResponseEvent createSuccessCacheResponse(String requestId,
                                                                  String url,
                                                                  String checksum,
                                                                  long size,
                                                                  Integer height,
                                                                  Integer width,
                                                                  String finalArchiveParentUrl,
                                                                  String fileCachePath) {
        return new StorageResponseEvent(requestId,
                                        url,
                                        checksum,
                                        size,
                                        height,
                                        width,
                                        finalArchiveParentUrl,
                                        fileCachePath);
    }

    /**
     * Success Reference event
     */
    public static StorageResponseEvent createSuccessReferenceResponse(String requestId, String url, String checksum) {
        return new StorageResponseEvent(requestId, url, checksum);
    }

    /**
     * Error event
     */
    public static StorageResponseEvent createErrorResponse(String requestId,
                                                           String url,
                                                           String checksum,
                                                           StorageResponseErrorEnum errorType,
                                                           String error) {
        return new StorageResponseEvent(requestId, url, checksum, errorType, error);
    }

    /**
     * Error event without indicate url and checksum
     */
    public static StorageResponseEvent createSimpleErrorResponse(String requestId,
                                                                 StorageResponseErrorEnum errorType,
                                                                 String error) {
        return new StorageResponseEvent(requestId, null, null, errorType, error);
    }
}
