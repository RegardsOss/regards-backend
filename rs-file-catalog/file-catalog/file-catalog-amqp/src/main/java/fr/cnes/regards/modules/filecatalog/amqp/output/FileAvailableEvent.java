/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.amqp.output;

import fr.cnes.regards.framework.amqp.event.IEvent;
import fr.cnes.regards.modules.fileaccess.dto.availability.FileAvailabilityStatusDto;

import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;

/**
 * Event raised to inform external applications (like GDH) that a file is now ready to download
 *
 * @author Thomas GUILLOU
 **/
// No need to @Event annotation because this event is not used internally
public class FileAvailableEvent extends FileAvailabilityStatusDto implements IEvent {

    public static final String ROUTING_KEY_AVAILABILITY_STATUS = "regards.file.availability.status";

    public static final String EXCHANGE_NAME = "regards.storage.file.notification";

    public FileAvailableEvent(String checksum, boolean available, @Nullable OffsetDateTime expirationDate) {
        super(checksum, available, expirationDate);
    }

    @Override
    public String toString() {
        return "FileAvailableEvent ["
               + "checksum='"
               + checksum
               + '\''
               + ", available="
               + available
               + ", expirationDate="
               + expirationDate
               + ']';
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o) && o.getClass().equals(FileAvailableEvent.class);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
