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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;

/**
 * S3 server to handle rs-order deliveries.
 *
 * @author Iliana Ghazali
 **/
public record S3DeliveryServer(
    // Scheme to access S3 server (http, https ...)
    @NotBlank(message = "Scheme must be present.") String scheme,
    // Host to access S3 server
    @NotBlank(message = "S3 host must be present.") String host,
    // S3 server port
    @Positive(message = "S3 port must be present and valid.") Integer port,
    // Region
    @NotBlank(message = "region must be present.") String region,
    // connection login
    @NotBlank(message = "key must be present.") String key,
    // s3 server secret
    @NotBlank(message = "secret must be present.") String secret) {

}
