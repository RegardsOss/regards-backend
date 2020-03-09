/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.client;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.storage.domain.IUpdateFileReferenceOnAvailable;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

/**
 * @author sbinda
 *
 */
@Component
public class AvailabilityUpdateCustomTestAction implements IUpdateFileReferenceOnAvailable {

    public static final String FILE_TO_UPDATE_NAME = "fileToUpdateAction.test";

    public static final String FILE_TO_UPDATE_CHECKSUM = "12345678912345";

    /* (non-Javadoc)
     * @see fr.cnes.regards.modules.storage.domain.IUpdateFileReferenceOnAvailable#update(fr.cnes.regards.modules.storage.domain.database.FileReference)
     */
    @Override
    public FileReference update(FileReference availableFileReference, FileLocation onlineFileLocation)
            throws ModuleException {
        // Update checksum of the restored file
        if (availableFileReference.getMetaInfo().getFileName().equals(FILE_TO_UPDATE_NAME)) {
            availableFileReference.getMetaInfo()
                    .setChecksum(getUpdatedChecksum(availableFileReference.getMetaInfo().getChecksum()));
            return availableFileReference;
        } else {
            return null;
        }
    }

    public static String getUpdatedChecksum(String checksum) {
        return "updated_" + checksum;
    }

}
