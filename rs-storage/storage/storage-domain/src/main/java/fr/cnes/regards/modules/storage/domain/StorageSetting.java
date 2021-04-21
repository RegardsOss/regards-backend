/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;
import java.util.List;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

public final class StorageSetting {

    public static final String RATE_LIMIT_NAME = "rateLimit";

    public static final String MAX_QUOTA_NAME = "maxQuota";

    public static final DynamicTenantSetting MAX_QUOTA = new DynamicTenantSetting(MAX_QUOTA_NAME,
                                                                                  "Default max quota for RAWDATA download. Must be > -1.",
                                                                                  -1L);

    public static final DynamicTenantSetting RATE_LIMIT = new DynamicTenantSetting(RATE_LIMIT_NAME,
                                                                                   "Default rate limit for RAWDATA download. Must be > -1.",
                                                                                   -1L);

    public static final List<DynamicTenantSetting> SETTING_LIST = Arrays.asList(
            MAX_QUOTA,
            RATE_LIMIT
    );

    private StorageSetting() {
    }
}
