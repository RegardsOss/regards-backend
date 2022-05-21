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
package fr.cnes.regards.modules.storage.domain;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public final class StorageSetting {

    public static final String RATE_LIMIT_NAME = "rateLimit";

    public static final String MAX_QUOTA_NAME = "maxQuota";

    public static final String CACHE_PATH_NAME = "tenantCachePath";

    public static final String CACHE_MAX_SIZE_NAME = "cacheMaxSize";

    public static final DynamicTenantSetting MAX_QUOTA = new DynamicTenantSetting(MAX_QUOTA_NAME,
                                                                                  "Default max quota for RAWDATA download. Must be > -1.",
                                                                                  -1L);

    public static final DynamicTenantSetting RATE_LIMIT = new DynamicTenantSetting(RATE_LIMIT_NAME,
                                                                                   "Default rate limit for RAWDATA download. Must be > -1.",
                                                                                   -1L);

    public static final Path DEFAULT_CACHE_ROOT = Paths.get("cache");

    public static final DynamicTenantSetting CACHE_PATH = new DynamicTenantSetting(CACHE_PATH_NAME,
                                                                                   "Cache path for this tenant. The path does not need to exist but it has to be readable and writable if it does.",
                                                                                   DEFAULT_CACHE_ROOT);

    public static final DynamicTenantSetting CACHE_MAX_SIZE = new DynamicTenantSetting(CACHE_MAX_SIZE_NAME,
                                                                                       "Cache max size for this tenant in kilo-bytes. it has to be >0",
                                                                                       500000000L);

    public static final List<DynamicTenantSetting> SETTING_LIST = Arrays.asList(MAX_QUOTA,
                                                                                RATE_LIMIT,
                                                                                CACHE_PATH,
                                                                                CACHE_MAX_SIZE);

    private StorageSetting() {
    }
}
