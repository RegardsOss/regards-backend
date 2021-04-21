/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.storage.domain.database.FileReference;

/**
 * Bus message to inform that a {@link FileReference} has been updated.
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class FileReferenceUpdateEvent implements ISubscribable {

    private String checksum;

    private String storage;

    private FileReference updatedFile;

    public static FileReferenceUpdateEvent build(String checksum, String storage, FileReference fileUpdated) {
        FileReferenceUpdateEvent event = new FileReferenceUpdateEvent();
        event.checksum = checksum;
        event.storage = storage;
        event.updatedFile = fileUpdated;
        return event;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * @param checksum the checksum to set
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the storage
     */
    public String getStorage() {
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public void setStorage(String storage) {
        this.storage = storage;
    }

    /**
     * @return the updatedFile
     */
    public FileReference getUpdatedFile() {
        return updatedFile;
    }

    /**
     * @param updatedFile the updatedFile to set
     */
    public void setUpdatedFile(FileReference updatedFile) {
        this.updatedFile = updatedFile;
    }

}
