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

/**
 * @author Thibaud Michaudel
 **/

public class S3Server {

    private String endpoint;

    private String region;

    private String key;

    private String secret;

    private String bucket;

    private String pattern;

    public S3Server() {
    }

    public S3Server(String endpoint, String region, String key, String secret, String bucket, String pattern) {
        this.endpoint = endpoint;
        this.region = region;
        this.key = key;
        this.secret = secret;
        this.bucket = bucket;
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
}
