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
package fr.cnes.regards.framework.s3.domain;

import javax.annotation.Nullable;
import java.time.ZonedDateTime;

/**
 * Information about a file stored in the Glacier (storage in S3 server: nearline type).
 *
 * @author Stephane Cortine
 */
public class GlacierFileStatus {

    private final RestorationStatus status;

    private final Long fileSize;

    /**
     * Expiration date of the file into the glacier
     */
    private final ZonedDateTime expirationDate;

    public GlacierFileStatus(RestorationStatus status, Long fileSize, @Nullable ZonedDateTime expirationDate) {
        this.status = status;
        this.fileSize = fileSize;
        this.expirationDate = expirationDate;
    }

    public RestorationStatus getStatus() {
        return status;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public ZonedDateTime getExpirationDate() {
        return expirationDate;
    }

    @Override
    public String toString() {
        return "GlacierFileStatus{"
               + "status="
               + status
               + ", fileSize="
               + fileSize
               + ", expirationDate="
               + expirationDate
               + '}';
    }
}
