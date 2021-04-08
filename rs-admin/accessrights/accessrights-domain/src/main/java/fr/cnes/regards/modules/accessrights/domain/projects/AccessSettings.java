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
package fr.cnes.regards.modules.accessrights.domain.projects;

import fr.cnes.regards.framework.modules.tenant.settings.domain.DynamicTenantSetting;
import fr.cnes.regards.framework.security.role.DefaultRole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class AccessSettings {

    private AccessSettings() {
    }

    public static final AcceptanceMode DEFAULT_MODE = AcceptanceMode.AUTO_ACCEPT;

    public static final DynamicTenantSetting MODE_SETTING = new DynamicTenantSetting(
            "acceptance_mode",
            "Acceptance Mode",
            DEFAULT_MODE.getName()
    );

    public static final DynamicTenantSetting DEFAULT_ROLE_SETTING = new DynamicTenantSetting(
            "default_role",
            "Default Role",
            DefaultRole.REGISTERED_USER.toString()
    );

    public static final DynamicTenantSetting DEFAULT_GROUPS_SETTING = new DynamicTenantSetting(
            "default_groups",
            "Default Groups",
            new ArrayList<>()
    );

    public static final List<DynamicTenantSetting> SETTING_LIST = Arrays.asList(
            MODE_SETTING,
            DEFAULT_ROLE_SETTING,
            DEFAULT_GROUPS_SETTING
    );

    public enum AcceptanceMode {

        MANUAL("manual"),
        AUTO_ACCEPT("auto-accept");

        private final String name;

        AcceptanceMode(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static AcceptanceMode fromName(String name) {
            for (AcceptanceMode mode : values()) {
                if (mode.getName().equals(name)) {
                    return mode;
                }
            }
            return null;
        }
    }

}
