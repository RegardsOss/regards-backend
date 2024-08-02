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

package fr.cnes.regards.framework.modules.dump.domain;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public final class DumpSettings {

    private DumpSettings() {
    }

    public static final String DUMP_PARAMETERS = "dump_parameters";

    public static final String LAST_DUMP_REQ_DATE = "last_dump_req_date";

    public static final boolean DEFAULT_ACTIVE_MODULE = true;

    public static final String DEFAULT_CRON_TRIGGER = "0 0 0 1-7 * SUN";

    public static final String DEFAULT_DUMP_LOCATION = "";

    public static final OffsetDateTime DEFAULT_LAST_DUMP_REQ_DATE = OffsetDateTime.ofInstant(Instant.EPOCH,
                                                                                             ZoneId.systemDefault());

    public static final DynamicTenantSetting DUMP_PARAMETERS_SETTING = new DynamicTenantSetting(DUMP_PARAMETERS,
                                                                                                "Dump parameters",
                                                                                                new DumpParameters().setActiveModule(
                                                                                                                        DEFAULT_ACTIVE_MODULE)
                                                                                                                    .setCronTrigger(
                                                                                                                        DEFAULT_CRON_TRIGGER)
                                                                                                                    .setDumpLocation(
                                                                                                                        DEFAULT_DUMP_LOCATION));

    public static final DynamicTenantSetting LAST_DUMP_REQ_DATE_SETTING = new DynamicTenantSetting(LAST_DUMP_REQ_DATE,
                                                                                                   "Date of last dump request",
                                                                                                   DEFAULT_LAST_DUMP_REQ_DATE);

    public static final List<DynamicTenantSetting> SETTING_LIST = Arrays.asList(DUMP_PARAMETERS_SETTING,
                                                                                LAST_DUMP_REQ_DATE_SETTING);

}
