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

package fr.cnes.regards.modules.ingest.domain.settings;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.Collections;
import java.util.List;

public final class AIPNotificationSettings {

    private AIPNotificationSettings() {
    }

    public static final String ACTIVE_NOTIFICATION = "active_notifications";

    public static final boolean DEFAULT_ACTIVE_NOTIFICATION = false;

    public static final DynamicTenantSetting ACTIVE_NOTIFICATION_SETTING = new DynamicTenantSetting(
            ACTIVE_NOTIFICATION,
            "Activate notifications on AIP request",
            DEFAULT_ACTIVE_NOTIFICATION
    );

    public static final List<DynamicTenantSetting> SETTING_LIST = Collections.singletonList(
            ACTIVE_NOTIFICATION_SETTING
    );

}
