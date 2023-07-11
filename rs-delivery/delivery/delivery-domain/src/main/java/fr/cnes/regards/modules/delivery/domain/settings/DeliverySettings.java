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
package fr.cnes.regards.modules.delivery.domain.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;

/**
 * List of {@link DynamicTenantSetting} to configure the delivery microservice
 *
 * @author Iliana Ghazali
 **/
public final class DeliverySettings {

    //-----------------------
    // SETTING NAMES
    //-----------------------

    public static final String REQUEST_TTL_HOURS = "request_ttl"; // request time to live

    public static final String S3_SERVER = "s3_server";

    public static final String BUILD_BUCKET = "build_bucket";

    public static final String DELIVERY_BUCKET = "delivery_bucket";

    //-----------------------
    // SETTING DEFAULT VALUES
    //-----------------------

    public static final int DEFAULT_REQUEST_TTL_HOURS = 12;

    public static final S3DeliveryServer DEFAULT_S3_SERVER = new S3DeliveryServer("rs-s3-minio",
                                                                                  9000,
                                                                                  "fr-regards-1",
                                                                                  "default-key",
                                                                                  "default-password");

    public static final String DEFAULT_BUILD_BUCKET = "default-build-bucket";

    public static final String DEFAULT_DELIVERY_BUCKET = "default-delivery-bucket";

    //-----------------------
    // SETTINGS INIT
    //-----------------------
    public static final DynamicTenantSetting REQUEST_TTL_HOURS_SETTING = new DynamicTenantSetting(REQUEST_TTL_HOURS,
                                                                                                  "Maximum retention time of a"
                                                                                                  + " delivery request in hours.",
                                                                                                  DEFAULT_REQUEST_TTL_HOURS);

    public static final DynamicTenantSetting S3_SERVER_SETTING = new DynamicTenantSetting(S3_SERVER,
                                                                                          "S3 server to place orders.",
                                                                                          DEFAULT_S3_SERVER);

    public static final DynamicTenantSetting BUILD_BUCKET_SETTING = new DynamicTenantSetting(BUILD_BUCKET,
                                                                                             "Temporary bucket on which available files will "
                                                                                             + "be transferred before building "
                                                                                             + "final zips.",
                                                                                             DEFAULT_BUILD_BUCKET);

    public static final DynamicTenantSetting DELIVERY_BUCKET_SETTING = new DynamicTenantSetting(DELIVERY_BUCKET,
                                                                                                "Bucket on which the "
                                                                                                + "ZIP archives will be dropped "
                                                                                                + "once orders have been completed.",
                                                                                                DEFAULT_DELIVERY_BUCKET);

    public static final List<DynamicTenantSetting> SETTING_LIST = List.of(REQUEST_TTL_HOURS_SETTING,
                                                                          S3_SERVER_SETTING,
                                                                          BUILD_BUCKET_SETTING,
                                                                          DELIVERY_BUCKET_SETTING);

    private DeliverySettings() {
        throw new IllegalStateException("Utility class to declare delivery settings.");
    }
}
