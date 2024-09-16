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
package fr.cnes.regards.modules.fileaccess.dto.output;

import java.util.Objects;

/**
 * Information about a storage request result. The request was successful if there is no error and errorType.
 *
 * @author Thibaud Michaudel
 **/
public class StorageResponseDto {

    private final Long requestId;

    private final String url;

    private final String checksum;

    private final long size;

    private final Integer height;

    private final Integer weight;

    private final boolean storedInCache;

    private final StorageResponseErrorEnum errorType;

    private final String error;

    /**
     * Full constructor
     */
    public StorageResponseDto(Long requestId,
                              String url,
                              String checksum,
                              long size,
                              Integer height,
                              Integer weight,
                              boolean storedInCache,
                              StorageResponseErrorEnum errorType,
                              String error) {
        this.requestId = requestId;
        this.url = url;
        this.checksum = checksum;
        this.size = size;
        this.height = height;
        this.weight = weight;
        this.storedInCache = storedInCache;
        this.errorType = errorType;
        this.error = error;
    }

    /**
     * Success constructor
     */
    public StorageResponseDto(Long requestId,
                              String url,
                              String checksum,
                              long size,
                              Integer height,
                              Integer weight,
                              boolean storedInCache) {
        this(requestId, url, checksum, size, height, weight, storedInCache, null, null);
    }

    /**
     * Error constructor
     */
    public StorageResponseDto(Long requestId,
                              String url,
                              String checksum,
                              StorageResponseErrorEnum errorType,
                              String error) {
        this(requestId, url, checksum, 0L, 0, 0, false, errorType, error);
    }

    /**
     * Reference success constructor
     */
    public StorageResponseDto(Long requestId, String url, String checksum) {
        this(requestId, url, checksum, 0, 0, 0, false);
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getUrl() {
        return url;
    }

    public String getChecksum() {
        return checksum;
    }

    public long getSize() {
        return size;
    }

    public Integer getHeight() {
        return height;
    }

    public Integer getWeight() {
        return weight;
    }

    public boolean isStoredInCache() {
        return storedInCache;
    }

    public StorageResponseErrorEnum getErrorType() {
        return errorType;
    }

    public boolean isRequestSuccessful() {
        return errorType == null && error == null;
    }

    public String getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageResponseDto that = (StorageResponseDto) o;
        return size == that.size
               && Objects.equals(height, that.height)
               && Objects.equals(weight, that.weight)
               && storedInCache == that.storedInCache
               && Objects.equals(requestId, that.requestId)
               && Objects.equals(url, that.url)
               && Objects.equals(checksum, that.checksum)
               && Objects.equals(errorType, that.errorType)
               && Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, url, checksum, size, height, weight, storedInCache, errorType, error);
    }

    @Override
    public String toString() {
        return "StorageResponseDto{"
               + "requestId="
               + requestId
               + ", url='"
               + url
               + '\''
               + ", checksum='"
               + checksum
               + '\''
               + ", size="
               + size
               + ", height="
               + height
               + ", weight="
               + weight
               + ", storedInCache="
               + storedInCache
               + ", errorType='"
               + errorType
               + '\''
               + ", error='"
               + error
               + '\''
               + '}';
    }
}
