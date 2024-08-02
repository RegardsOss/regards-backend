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
package fr.cnes.regards.modules.ltamanager.domain.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * List of {@link DynamicTenantSetting} to configure the lta-manager microservice
 *
 * @author Iliana Ghazali
 **/
public final class LtaSettings {

    //-----------------------
    // SETTING NAMES
    //-----------------------

    public static final String STORAGE_KEY = "storage";

    public static final String SUCCESS_EXPIRATION_IN_HOURS_KEY = "success_expiration_in_hours";

    public static final String DATATYPES_KEY = "datatypes";

    //-----------------------
    // SETTING DEFAULT VALUES
    //-----------------------

    public static final String DEFAULT_STORAGE = "LTA";

    public static final int DEFAULT_SUCCESS_EXPIRATION_HOURS = 24;

    public static final Map<String, DatatypeParameter> DEFAULT_DATATYPES = new HashMap<>();

    //-----------------------
    // SETTINGS INIT
    //-----------------------
    public static final DynamicTenantSetting STORAGE_SETTING = new DynamicTenantSetting(STORAGE_KEY,
                                                                                        "The storage location name.",
                                                                                        DEFAULT_STORAGE);

    public static final DynamicTenantSetting SUCCESS_EXPIRATION_SETTING = new DynamicTenantSetting(
        SUCCESS_EXPIRATION_IN_HOURS_KEY,
        "The lifetime of successful requests in hours.",
        DEFAULT_SUCCESS_EXPIRATION_HOURS);

    public static final DynamicTenantSetting DATATYPES_SETTING = new DynamicTenantSetting(DATATYPES_KEY,
                                                                                          "The datatype of incoming requests.",
                                                                                          DEFAULT_DATATYPES);

    public static final List<DynamicTenantSetting> SETTING_LIST = List.of(STORAGE_SETTING,
                                                                          SUCCESS_EXPIRATION_SETTING,
                                                                          DATATYPES_SETTING);

    private LtaSettings() {
        throw new IllegalStateException("Utility class to declare lta settings.");
    }
}
