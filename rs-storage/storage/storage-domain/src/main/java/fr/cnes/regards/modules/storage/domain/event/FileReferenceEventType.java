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
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * Type of {@link FileReferenceEvent}
 *
 * @author Sébastien Binda
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public enum FileReferenceEventType {

    /**
     * File has been successfully copied from one storage to another
     */
    COPIED,

    /**
     * Error during file copy
     */
    COPY_ERROR,

    /**
     * File has been successfully stored
     */
    STORED,

    /**
     * Error occurs during file reference storage
     */
    STORE_ERROR,

    /**
     * File reference does not belongs to owner
     */
    DELETED_FOR_OWNER,

    /**
     * File reference has been fully deleted
     */
    FULLY_DELETED,

    /**
     * Error occurs during file deletion on storage
     */
    DELETION_ERROR,

    /**
     * File reference is available for download
     */
    AVAILABLE,

    /**
     * File reference is not available for download
     */
    AVAILABILITY_ERROR;
}
