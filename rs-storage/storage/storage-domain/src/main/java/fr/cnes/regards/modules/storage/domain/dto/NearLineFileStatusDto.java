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
package fr.cnes.regards.modules.storage.domain.dto;

import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Dto used to indicate if a file is available to download, and other information
 *
 * @author Thomas GUILLOU
 **/
public class NearLineFileStatusDto {

    /**
     * file availability.</br>
     * if true, it means that the file is located in T2 or in the restoration cache
     */
    private boolean available;

    @Nullable
    private OffsetDateTime expirationDate;

    public NearLineFileStatusDto(boolean available, @Nullable OffsetDateTime expirationDate) {
        this.available = available;
        this.expirationDate = expirationDate;
    }

    public boolean isAvailable() {
        return available;
    }

    @Nullable
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
        NearLineFileStatusDto that = (NearLineFileStatusDto) o;
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
