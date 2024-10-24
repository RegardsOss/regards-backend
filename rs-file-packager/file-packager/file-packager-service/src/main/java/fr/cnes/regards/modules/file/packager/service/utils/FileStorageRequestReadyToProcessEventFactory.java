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
package fr.cnes.regards.modules.file.packager.service.utils;

import fr.cnes.regards.modules.fileaccess.amqp.input.FileStorageRequestReadyToProcessEvent;
import fr.cnes.regards.modules.fileaccess.dto.input.FileStorageMetaInfoDto;

import java.nio.file.Path;
import java.time.OffsetDateTime;

/**
 * Utils class to create {@link FileStorageRequestReadyToProcessEvent}
 *
 * @author Thibaud Michaudel
 **/
public class FileStorageRequestReadyToProcessEventFactory {

    private static final String JOB_REQUEST_OWNER = "FilePackager";

    private FileStorageRequestReadyToProcessEventFactory() {
    }

    /**
     * Create a package storage request event
     */
    public static FileStorageRequestReadyToProcessEvent createPackageRequestEvent(Long packageId,
                                                                                  String storageSubdirectory,
                                                                                  String storage,
                                                                                  String checksum,
                                                                                  Path archivePath) {
        OffsetDateTime today = OffsetDateTime.now();
        return new FileStorageRequestReadyToProcessEvent(packageId,
                                                         checksum,
                                                         "MD5",
                                                         archivePath.toString(),
                                                         storage,
                                                         storageSubdirectory,
                                                         JOB_REQUEST_OWNER,
                                                         ""
                                                         + today.getYear()
                                                         + today.getMonthValue()
                                                         + today.getDayOfMonth(),
                                                         false,
                                                         new FileStorageMetaInfoDto("application/zip", "RAWDATA", 0, 0),
                                                         false);
    }
}
