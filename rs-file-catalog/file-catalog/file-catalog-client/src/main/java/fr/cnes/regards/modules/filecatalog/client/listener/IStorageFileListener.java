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
package fr.cnes.regards.modules.filecatalog.client.listener;

import fr.cnes.regards.modules.fileaccess.amqp.input.FilesDeletionEvent;
import fr.cnes.regards.modules.fileaccess.amqp.input.FilesRestorationRequestEvent;
import fr.cnes.regards.modules.fileaccess.amqp.output.FileReferenceEvent;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceUpdateDto;

import java.util.List;

/**
 * Listener to handle bus messages from storage service.
 *
 * @author Sébastien Binda
 */
public interface IStorageFileListener {

    /**
     * Callback when a file is successfully stored or referenced
     */
    void onFileStored(List<FileReferenceEvent> stored);

    /**
     * Callback when a file to store is in error.
     */
    void onFileStoreError(List<FileReferenceEvent> storedError);

    /**
     * Callback when a file is available for download. Next to a {@link FilesRestorationRequestEvent} request.
     */
    void onFileAvailable(List<FileReferenceEvent> available);

    /**
     * Callback when a file is not available for download. Next to a {@link FilesRestorationRequestEvent} request.
     */
    void onFileNotAvailable(List<FileReferenceEvent> availabilityError);

    /**
     * Callback when a file is successfully deleted. Next to a {@link FilesDeletionEvent} request.
     */
    void onFileDeletedForOwner(String owner, List<FileReferenceEvent> deletedForThisOwner);

    /**
     * Callback when a file is updated.
     */
    void onFileUpdated(List<FileReferenceUpdateDto> updatedReferences);
}
