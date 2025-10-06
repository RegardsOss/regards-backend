/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.modules.storage.service.file.request;

import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestDto;
import fr.cnes.regards.modules.storage.domain.database.request.FileStorageRequestAggregation;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.Optional;

/**
 * Helper methods related to file storage request dtos and entities.
 * Currently used by {@link FileStorageRequestService}
 *
 * @author Olivier Navarro
 **/
public final class FileStorageRequestUtils {

    /**
     * static class not meant to be instantiated.
     */
    private FileStorageRequestUtils() {
    }

    /**
     * Find the most valuable/discriminative request (based on the status) from a collection of requests matching the
     * storage and checksum of a given request to handle.
     */
    public static Optional<FileStorageRequestAggregation> findMostRelevantRequest(FileStorageRequestDto requestToHandle,
                                                                                  Collection<FileStorageRequestAggregation> requests) {
        final FileStorageRequestAggregation found = requests.stream()
                                                            // same checksum and storage
                                                            .filter(request -> haveSameStorageAndChecksum(
                                                                requestToHandle,
                                                                request))
                                                            .reduce(null,
                                                                    FileStorageRequestUtils::pickMostRelevantRequest);
        return Optional.ofNullable(found);
    }

    /**
     * Pick the most valuable request between the two given ones according the ordering of their status:
     * SUCCESS = ERROR > DELAYED > TO_DO > PENDING.
     * Assume both request have identical storage and checksum.
     *
     * @param request1 1st request
     * @param request2 2dn request
     * @return {@link FileStorageRequestAggregation}: either request1 or request1
     */
    public static FileStorageRequestAggregation pickMostRelevantRequest(FileStorageRequestAggregation request1,
                                                                        FileStorageRequestAggregation request2) {
        if (request1 == null) {
            return request2;
        }
        // ERROR > DELAYED > TO_DO > PENDING.
        return switch (request1.getStatus()) {
            case SUCCESS, ERROR, DELAYED, TO_DO -> request1;
            case PENDING -> request2;
        };
    }

    /**
     * Compare the checksum and storage of the dto {@link FileStorageRequestDto} and
     * the entity {@link FileStorageRequestAggregation}.
     *
     * @param requestDto    the dto {@link FileStorageRequestDto} having its checksum and storage compared.
     * @param requestEntity the entity {@link FileStorageRequestAggregation} having its checksum and storage compared.
     * @return a boolean indicating whether the checksum and storage are the same or not between the dto and entity.
     */
    public static boolean haveSameStorageAndChecksum(FileStorageRequestDto requestDto,
                                                     FileStorageRequestAggregation requestEntity) {
        return
            // same checksum
            StringUtils.equals(requestEntity.getMetaInfo().getChecksum(), requestDto.getChecksum())
            // and same storage
            && StringUtils.equals(requestEntity.getStorage(), requestDto.getStorage());
    }
}


