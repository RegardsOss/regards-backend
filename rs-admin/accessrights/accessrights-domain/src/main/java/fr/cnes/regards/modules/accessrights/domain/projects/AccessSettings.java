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

import java.util.List;
import java.util.Objects;

public class AccessSettings {

    public static final String MODE_SETTING = "acceptance_mode";
    public static final String DEFAULT_ROLE_SETTING = "default_role";
    public static final String DEFAULT_GROUPS_SETTING = "default_groups";

    public static final AcceptanceMode DEFAULT_MODE = AcceptanceMode.AUTO_ACCEPT;

    private String mode;
    private String defaultRole;
    private List<String> defaultGroups;


    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }

    public List<String> getDefaultGroups() {
        return defaultGroups;
    }

    public void setDefaultGroups(List<String> defaultGroups) {
        this.defaultGroups = defaultGroups;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccessSettings)) return false;
        AccessSettings that = (AccessSettings) o;
        return mode.equals(that.mode) && Objects.equals(defaultRole, that.defaultRole) && Objects.equals(defaultGroups, that.defaultGroups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, defaultRole, defaultGroups);
    }

}
