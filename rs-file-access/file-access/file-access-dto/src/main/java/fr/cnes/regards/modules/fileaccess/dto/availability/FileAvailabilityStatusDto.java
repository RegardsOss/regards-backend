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
package fr.cnes.regards.modules.fileaccess.dto.availability;

import com.google.gson.annotations.SerializedName;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Availability status of a file
 *
 * @author Thomas GUILLOU
 **/
public class FileAvailabilityStatusDto {

    /**
     * md5 of file
     */
    protected final String checksum;

    /**
     * file availability.</br>
     * if true, it means that the file is located in T2 or in the restoration cache (or is in online storage)
     */
    @Schema(description = "File availability")
    protected final boolean available;

    @SerializedName(value = "expiration_date")
    @Nullable
    @Schema(name = "expiration_date", description = "Indicate date when the file will be not available anymore.")
    protected final OffsetDateTime expirationDate;

    public FileAvailabilityStatusDto(String checksum, boolean available, @Nullable OffsetDateTime expirationDate) {
        this.checksum = checksum;
        this.available = available;
        this.expirationDate = expirationDate;
    }

    public String getChecksum() {
        return checksum;
    }

    public boolean isAvailable() {
        return available;
    }

    //    @Nullable
    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileAvailabilityStatusDto that = (FileAvailabilityStatusDto) o;
        return available == that.available && Objects.equals(checksum, that.checksum) && Objects.equals(expirationDate,
                                                                                                        that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(checksum, available, expirationDate);
    }

    @Override
    public String toString() {
        return "FileAvailabilityStatusDto ["
               + "checksum='"
               + checksum
               + '\''
               + ", available="
               + available
               + ", expirationDate="
               + expirationDate
               + ']';
    }
}
