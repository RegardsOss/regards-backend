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

package fr.cnes.regards.modules.storage.service.file.fixture;

import fr.cnes.regards.modules.fileaccess.dto.FileRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.util.UUID;

import static fr.cnes.regards.modules.storage.service.file.fixture.FileReferenceConstants.*;

/**
 * @author Olivier Navarro
 **/
@Value
@Builder
@With
@AllArgsConstructor
public class FileReferenceRequestArgs {

    String fileName;

    String checksum;

    String algorithm;

    String mimeType;

    String owner;

    String storage;

    String groupId;

    String url;

    String type;

    String sessionOwner;

    String session;

    Long fileSize;

    Integer height;

    Integer width;

    String subDirectory;

    @Builder.Default
    FileRequestStatus expectedStatus = FileRequestStatus.SUCCESS;

    public static FileReferenceRequestArgs newFileReferenceRequestArgs1() {
        return FileReferenceRequestArgs.builder()
                                       .checksum(CHECKSUM1)
                                       .owner(OWNER1)
                                       .fileName(FILE_REF_NAME)
                                       .storage(STORAGE1)
                                       .url(URL1)
                                       .sessionOwner(SESSION1_OWNER)
                                       .session(SESSION1)
                                       .groupId(UUID.randomUUID().toString())
                                       .build();
    }
}
