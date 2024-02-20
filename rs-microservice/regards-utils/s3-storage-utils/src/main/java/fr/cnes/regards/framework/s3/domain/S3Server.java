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
package fr.cnes.regards.framework.s3.domain;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;

/**
 * Settings class for a S3 server.
 *
 * @author Thibaud Michaudel
 **/

public class S3Server {

    public static final String REGEX_GROUP_BUCKET = "bucket";

    public static final String REGEX_GROUP_PATHFILENAME = "pathWithFilename";

    private static final String DEFAULT_PATTERN = "http[s]{0,1}://(?:.*?)/(?<"
                                                  + REGEX_GROUP_BUCKET
                                                  + ">.*?)/(?<"
                                                  + REGEX_GROUP_PATHFILENAME
                                                  + ">.*)";

    /**
     * Url of endpoint for the S3 server
     */
    @NotBlank(message = "Endpoint is required.")
    private String endpoint;

    @NotBlank(message = "Region is required.")
    private String region;

    @NotBlank(message = "Key is required.")
    private String key;

    @NotBlank(message = "Secret is required.")
    private String secret;

    private String bucket;

    /**
     * Pattern for regex to retrieve bucket(named group: {@link S3Server#REGEX_GROUP_BUCKET}) and path with filename(named group: {@link S3Server#REGEX_GROUP_PATHFILENAME}) from url.
     * If the bucket field {@link S3Server#bucket} is null, this pattern allows to retrieve it.
     * <p>
     * By default, the pattern is {@link S3Server#DEFAULT_PATTERN}
     */
    private String pattern = DEFAULT_PATTERN;

    /**
     * Configure the maximum number of times that a single request should be retried by the AWS S3 SDK, assuming it
     * fails for a retryable error.
     */
    @Nullable
    private Integer maxRetriesNumber;

    /**
     * Duration in seconds used to delay before trying another time when retryable error occurs
     * Used by {@link software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy}
     * See examples here {@link software.amazon.awssdk.core.retry.backoff.BackoffStrategy#defaultThrottlingStrategy(software.amazon.awssdk.core.retry.RetryMode)}
     */
    @Nullable
    private Integer retryBackOffBaseDuration;

    /**
     * Duration in seconds used to compute a random delay before trying another time when retryable error occurs
     * Used by {@link software.amazon.awssdk.core.retry.backoff.EqualJitterBackoffStrategy}
     * See examples here {@link software.amazon.awssdk.core.retry.backoff.BackoffStrategy#defaultThrottlingStrategy(software.amazon.awssdk.core.retry.RetryMode)}
     */
    @Nullable
    private Integer retryBackOffMaxDuration;

    public S3Server() {
    }

    public S3Server(String endpoint, String region, String key, String secret, String bucket) {
        this.endpoint = endpoint;
        this.region = region;
        this.key = key;
        this.secret = secret;
        this.bucket = bucket;
    }

    public S3Server(String endpoint, String region, String key, String secret, String bucket, String pattern) {
        this(endpoint, region, key, secret, bucket);
        this.pattern = pattern;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public S3Server setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public S3Server setRegion(String region) {
        this.region = region;
        return this;
    }

    public String getKey() {
        return key;
    }

    public S3Server setKey(String key) {
        this.key = key;
        return this;
    }

    public String getSecret() {
        return secret;
    }

    public S3Server setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    public String getBucket() {
        return bucket;
    }

    public S3Server setBucket(String bucket) {
        this.bucket = bucket;
        return this;
    }

    public String getPattern() {
        return pattern;
    }

    public S3Server setPattern(String pattern) {
        this.pattern = pattern;
        return this;
    }

    public Integer getMaxRetriesNumber() {
        return maxRetriesNumber;
    }

    public void setMaxRetriesNumber(Integer maxRetriesNumber) {
        this.maxRetriesNumber = maxRetriesNumber;
    }

    public Integer getRetryBackOffBaseDuration() {
        return retryBackOffBaseDuration;
    }

    public void setRetryBackOffBaseDuration(Integer retryBackOffBaseDuration) {
        this.retryBackOffBaseDuration = retryBackOffBaseDuration;
    }

    public Integer getRetryBackOffMaxDuration() {
        return retryBackOffMaxDuration;
    }

    public void setRetryBackOffMaxDuration(Integer retryBackOffMaxDuration) {
        this.retryBackOffMaxDuration = retryBackOffMaxDuration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3Server s3Server = (S3Server) o;

        return endpoint.equals(s3Server.endpoint);
    }

    @Override
    public int hashCode() {
        return endpoint.hashCode();
    }

    @Override
    public String toString() {
        return "S3Server{"
               + "endpoint='"
               + endpoint
               + '\''
               + ", region='"
               + region
               + '\''
               + ", key='"
               + key
               + '\''
               + ", secret='"
               + secret
               + '\''
               + ", bucket='"
               + bucket
               + '\''
               + ", pattern='"
               + pattern
               + '\''
               + '}';
    }
}
