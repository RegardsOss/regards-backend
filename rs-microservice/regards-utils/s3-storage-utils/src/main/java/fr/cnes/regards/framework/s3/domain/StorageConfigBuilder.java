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
package fr.cnes.regards.framework.s3.domain;

import fr.cnes.regards.framework.s3.dto.StorageConfigDto;

import javax.annotation.Nullable;

/**
 * @author Thibaud Michaudel
 **/
public class StorageConfigBuilder {

    private String endpoint;

    private String region;

    private String key;

    private String secret;

    private String bucket;

    private String rootPath;

    /**
     * Configure the maximum number of times that a single request should be retried by the AWS S3 SDK, assuming it
     * fails for a retryable error.
     */
    private Integer maxRetriesNumber = 4;

    /**
     * Duration in seconds used to delay before trying another time when retryable error occurs
     * Used by {@link software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy}
     * See examples here {@link software.amazon.awssdk.core.retry.backoff.BackoffStrategy#defaultThrottlingStrategy(software.amazon.awssdk.core.retry.RetryMode)}
     */
    private Integer retryBackOffBaseDuration = 1;

    /**
     * Duration in seconds used to compute a random delay before trying another time when retryable error occurs
     * Used by {@link software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy}
     * See examples here {@link software.amazon.awssdk.core.retry.backoff.BackoffStrategy#defaultThrottlingStrategy(software.amazon.awssdk.core.retry.RetryMode)}
     */
    private Integer retryBackOffMaxDuration = 20;

    public StorageConfigBuilder(String endpoint, String region, String key, String secret) {
        this.endpoint = endpoint;
        this.region = region;
        this.key = key;
        this.secret = secret;
    }

    public StorageConfigBuilder(S3Server s3Server) {
        this.endpoint = s3Server.getEndpoint();
        this.key = s3Server.getKey();
        this.region = s3Server.getRegion();
        this.secret = s3Server.getSecret();
        this.bucket = s3Server.getBucket();
    }

    public StorageConfigBuilder bucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public StorageConfigBuilder rootPath(String rootPath) {
        this.rootPath = rootPath;
        return this;
    }

    public StorageConfigBuilder maxRetriesNumber(int maxRetriesNumber) {
        this.maxRetriesNumber = maxRetriesNumber;
        return this;
    }

    public StorageConfigBuilder maxRetriesNumber(@Nullable Integer maxRetriesNumber) {
        if (maxRetriesNumber != null) {
            this.maxRetriesNumber = maxRetriesNumber;
        }
        return this;
    }

    public StorageConfigBuilder retryBackOffBaseDuration(int retryBackOffBaseDuration) {
        this.retryBackOffBaseDuration = retryBackOffBaseDuration;
        return this;
    }

    public StorageConfigBuilder retryBackOffBaseDuration(@Nullable Integer retryBackOffMaxDuration) {
        if (retryBackOffMaxDuration != null) {
            this.retryBackOffMaxDuration = retryBackOffMaxDuration;
        }
        return this;
    }

    public StorageConfigBuilder retryBackOffMaxDuration(int retryBackOffMaxDuration) {
        this.retryBackOffMaxDuration = retryBackOffMaxDuration;
        return this;
    }

    public StorageConfigBuilder retryBackOffMaxDuration(@Nullable Integer retryBackOffBaseDuration) {
        if (retryBackOffBaseDuration != null) {
            this.retryBackOffBaseDuration = retryBackOffBaseDuration;
        }

        return this;
    }

    public StorageConfigDto build() {
        return new StorageConfigDto(endpoint,
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
