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
package fr.cnes.regards.framework.s3.dto;

import java.util.Objects;

/**
 * @author Thibaud Michaudel
 **/
public class StorageConfigDto {

    private final String endpoint;

    private final String region;

    private final String key;

    private final String secret;

    private final String bucket;

    private final String rootPath;

    private final Integer maxRetriesNumber;

    private final Integer retryBackOffBaseDuration;

    private final Integer retryBackOffMaxDuration;

    public StorageConfigDto(String endpoint,
                            String region,
                            String key,
                            String secret,
                            String bucket,
                            String rootPath,
                            Integer maxRetriesNumber,
                            Integer retryBackOffBaseDuration,
                            Integer retryBackOffMaxDuration) {
        this.endpoint = endpoint;
        this.region = region;
        this.key = key;
        this.secret = secret;
        this.bucket = bucket;
        this.rootPath = rootPath;
        this.maxRetriesNumber = maxRetriesNumber;
        this.retryBackOffBaseDuration = retryBackOffBaseDuration;
        this.retryBackOffMaxDuration = retryBackOffMaxDuration;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getRegion() {
        return region;
    }

    public String getKey() {
        return key;
    }

    public String getSecret() {
        return secret;
    }

    public String getBucket() {
        return bucket;
    }

    public String getRootPath() {
        return rootPath;
    }

    public Integer getMaxRetriesNumber() {
        return maxRetriesNumber;
    }

    public Integer getRetryBackOffBaseDuration() {
        return retryBackOffBaseDuration;
    }

    public Integer getRetryBackOffMaxDuration() {
        return retryBackOffMaxDuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageConfigDto that = (StorageConfigDto) o;
        return Objects.equals(endpoint, that.endpoint)
               && Objects.equals(region, that.region)
               && Objects.equals(key,
                                 that.key)
               && Objects.equals(secret, that.secret)
               && Objects.equals(bucket, that.bucket)
               && Objects.equals(rootPath, that.rootPath)
               && Objects.equals(maxRetriesNumber, that.maxRetriesNumber)
               && Objects.equals(retryBackOffBaseDuration, that.retryBackOffBaseDuration)
               && Objects.equals(retryBackOffMaxDuration, that.retryBackOffMaxDuration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(endpoint,
                            region,
                            key,
                            secret,
                            bucket,
                            rootPath,
                            maxRetriesNumber,
                            retryBackOffBaseDuration,
                            retryBackOffMaxDuration);
    }
}
