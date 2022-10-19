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

package fr.cnes.regards.modules.ingest.domain.settings;

import com.google.common.collect.ImmutableList;
import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.util.List;

public final class IngestSettings {

    private IngestSettings() {
    }

    //-------------------- Sip body time to live --------------------

    public static final String SIP_BODY_TIME_TO_LIVE = "sip_body_time_to_live";

    public static int DEFAULT_SIP_BODY_TIME_TO_LIVE_IN_DAYS = 7;

    private static final DynamicTenantSetting SIP_BODY_TIME_TO_LIVE_SETTING = new DynamicTenantSetting(
        SIP_BODY_TIME_TO_LIVE,
        "Life time of a SIP, in days. SIP will be automatically removed at the term.",
        DEFAULT_SIP_BODY_TIME_TO_LIVE_IN_DAYS);

    //-------------------- Active notification --------------------
    public static final String ACTIVE_NOTIFICATION = "active_notifications";

    public static final boolean DEFAULT_ACTIVE_NOTIFICATION = false;

    private static final DynamicTenantSetting ACTIVE_NOTIFICATION_SETTING = new DynamicTenantSetting(ACTIVE_NOTIFICATION,
                                                                                                     "Activate notifications on AIP request",
                                                                                                     DEFAULT_ACTIVE_NOTIFICATION);

    public static final List<DynamicTenantSetting> SETTING_LIST = ImmutableList.of(ACTIVE_NOTIFICATION_SETTING,
                                                                                   SIP_BODY_TIME_TO_LIVE_SETTING);
}
