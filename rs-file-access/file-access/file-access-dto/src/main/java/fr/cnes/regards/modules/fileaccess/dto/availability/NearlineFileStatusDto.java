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
package fr.cnes.regards.modules.fileaccess.dto.availability;

import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Dto used to indicate if a file is available to download, and other information
 *
 * @author Thomas GUILLOU
 **/
public class NearlineFileStatusDto {

    private final String checksum;

    /**
     * File availability status.
     * <ul>
     * <li>AVAILABLE : File is available for download</li>
     * <li>UNAVAILABLE : File is not available for download. A restore action must be processed first.</li>
     * <li>ERROR : Error during availability status. File could be available or not.</li>
     * </ul>
     */
    private final NearlineFileStatusDtoStatus available;

    @Nullable
    private final OffsetDateTime expirationDate;

    @Nullable
    private String message;

    public NearlineFileStatusDto(String checksum,
                                 NearlineFileStatusDtoStatus available,
                                 @Nullable OffsetDateTime expirationDate,
                                 @Nullable String message) {
        this.checksum = checksum;
        this.available = available;
        this.expirationDate = expirationDate;
        this.message = message;
    }

    public String getChecksum() {
        return checksum;
    }

    public NearlineFileStatusDtoStatus getAvailable() {
        return available;
    }

    @Nullable
    public OffsetDateTime getExpirationDate() {
        return expirationDate;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NearlineFileStatusDto that = (NearlineFileStatusDto) o;
        return available == that.available && Objects.equals(expirationDate, that.expirationDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(available, expirationDate);
    }

    @Override
    public String toString() {
        return "NearLineFileStatusDto{" + "available=" + available + ", expirationDate=" + expirationDate + '}';
    }
}
