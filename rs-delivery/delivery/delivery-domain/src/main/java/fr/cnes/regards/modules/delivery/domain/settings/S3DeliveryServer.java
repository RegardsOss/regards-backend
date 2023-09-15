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
package fr.cnes.regards.modules.delivery.domain.settings;

import fr.cnes.regards.framework.encryption.sensitive.StringSensitive;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import java.util.Objects;

/**
 * S3 server to handle rs-order deliveries.
 *
 * @author Iliana Ghazali
 **/
public final class S3DeliveryServer {

    /**
     * Scheme to access S3 server (http, https ...)
     */
    @NotBlank(message = "Scheme must be present.")
    private final String scheme;

    /**
     * Host to access S3 server
     */
    @NotBlank(message = "S3 host must be present.")
    private final String host;

    /**
     * S3 server port
     */
    @Positive(message = "S3 port must be present and valid.")
    private final Integer port;

    /**
     * Server region
     */
    @NotBlank(message = "region must be present.")
    private final String region;

    /**
     * S3 connection login
     */
    @NotBlank(message = "key must be present.")
    private final String key;

    /**
     * S3 server secret
     */
    @NotBlank(message = "secret must be present.")
    @StringSensitive
    private String secret;

    public S3DeliveryServer(String scheme, String host, Integer port, String region, String key, String secret) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.region = region;
        this.key = key;
        this.secret = secret;
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
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

    public void setSecret(String secret) {
        this.secret = secret;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (S3DeliveryServer) obj;
        return Objects.equals(this.scheme, that.scheme)
               && Objects.equals(this.host, that.host)
               && Objects.equals(this.port, that.port)
               && Objects.equals(this.region, that.region)
               && Objects.equals(this.key, that.key)
               && Objects.equals(this.secret, that.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, host, port, region, key, secret);
    }

    @Override
    public String toString() {
        return "S3DeliveryServer{"
               + "scheme='"
               + scheme
               + '\''
               + ", host='"
               + host
               + '\''
               + ", port="
               + port
               + ", region='"
               + region
               + '\''
               + ", key='"
               + key
               + '\''
               + ", secret is hidden'}'"
               + secret;
    }
}
