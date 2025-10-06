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

import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileReferenceRequestDto;
import fr.cnes.regards.modules.fileaccess.dto.request.FileStorageRequestAggregationDto;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;

/**
 * Helper for building {@link FileReferenceRequestDto}
 *
 * @author Olivier Navarro
 **/
public final class FileReferenceRequestDtoBuilders {

    /**
     * static class not meant to be instantiated.
     */
    private FileReferenceRequestDtoBuilders() {
    }

    /**
     * Build a {@link FileReferenceRequestDto} from the given {@link FileStorageRequestAggregationDto}.
     *
     * @param request the {@link FileStorageRequestAggregationDto} from which to build the
     *                {@link FileReferenceRequestDto}.
     * @return a new instance of {@link FileReferenceRequestDto}.
     */
    public static FileReferenceRequestDto toFileReferenceDto(FileStorageRequestAggregationDto request) {
        final FileReferenceMetaInfoDto metaInfo = request.getMetaInfo();
        final String owner = request.getOwners().stream().findFirst().orElse(null);
        final FileReferenceRequestDto dto = FileReferenceRequestDto.build(metaInfo.getFileName(),
                                                                          metaInfo.getChecksum(),
                                                                          metaInfo.getAlgorithm(),
                                                                          metaInfo.getMimeType(),
                                                                          metaInfo.getFileSize(),
                                                                          owner,
                                                                          request.getStorage(),
                                                                          request.getOriginUrl(),
                                                                          request.getSessionOwner(),
                                                                          request.getSession());
        dto.withHeight(metaInfo.getHeight()).withWidth(metaInfo.getWidth()).withType(metaInfo.getType());
        return dto;
    }

    /**
     * Build a {@link FileReferenceRequestDto} from the given parameters.
     *
     * @param owner        the owner
     * @param metaInfo     the meta info file {@link FileReferenceMetaInfo}
     * @param location     the file location {@link FileLocation}
     * @param sessionOwner the session owner
     * @param session      the session
     * @return a new instance of {@link FileReferenceRequestDto}.
     */
    public static FileReferenceRequestDto toFileReferenceDto(String owner,
                                                             FileReferenceMetaInfo metaInfo,
                                                             FileLocation location,
                                                             String sessionOwner,
                                                             String session) {
        final FileReferenceRequestDto dto = FileReferenceRequestDto.build(metaInfo.getFileName(),
                                                                          metaInfo.getChecksum(),
                                                                          metaInfo.getAlgorithm(),
                                                                          metaInfo.getMimeType().toString(),
                                                                          metaInfo.getFileSize(),
                                                                          owner,
                                                                          location.getStorage(),
                                                                          location.getUrl(),
                                                                          sessionOwner,
                                                                          session);
        dto.withHeight(metaInfo.getHeight()).withWidth(metaInfo.getWidth()).withType(metaInfo.getType());
        return dto;
    }
}
