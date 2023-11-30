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
package fr.cnes.regards.modules.storage.domain.event;

import fr.cnes.regards.framework.amqp.event.IEvent;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Event raised to inform external applications (like GDH) that a file is now ready to download
 *
 * @author Thomas GUILLOU
 * @see <a href="https://odin.si.c-s.fr/plugins/tracker/?aid=369460">odin task</a>
 **/
// No need to @Event annotation because this event is not used internally
public class FileAvailableEvent implements IEvent {

    public static final String ROUTING_KEY_AVAILABILITY_STATUS = "regards.file.availability.status";

    public static final String EXCHANGE_NAME = "regards.storage.file.notification";

    /**
     * md5 of file
     */
    private String checksum;

    /**
     * file availability.</br>
     * if true, it means that the file is located in T2 or in restoration cache
     * if false, ??
     */
    private boolean available;

    @Nullable
    private OffsetDateTime expirationDate;

    public static FileAvailableEvent build(String checksum, boolean available, OffsetDateTime expirationDate) {
        FileAvailableEvent fileAvailableEvent = new FileAvailableEvent();
        fileAvailableEvent.checksum = checksum;
        fileAvailableEvent.available = available;
        fileAvailableEvent.expirationDate = expirationDate;
        return fileAvailableEvent;
    }

    public String getChecksum() {
        return checksum;
    }

    public boolean isAvailable() {
        return available;
    }

    @Nullable
    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    @Override
    public String toString() {
        return "FileAvailableEvent{"
               + "checksum='"
               + checksum
               + '\''
               + ", available="
               + available
               + ", expirationDate="
               + expirationDate
               + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileAvailableEvent that = (FileAvailableEvent) o;
        return available == that.available && Objects.equals(checksum, that.checksum) && Objects.equals(expirationDate,
                                                                                                        that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, available, expirationDate);
    }
}
