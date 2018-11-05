/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.plugin;

/**
 * POJO for plugin configuration update informations.
 * If a plugin configuration can not be updated, this pojo must contains the reason.
 * @author Sébastien Binda
 */
public class PluginConfUpdatable {

    private final boolean updateAllowed;

    private final String updateNotAllowedReason;

    private PluginConfUpdatable(boolean updateAllowed, String updateNotAllowedReason) {
        this.updateAllowed = updateAllowed;
        this.updateNotAllowedReason = updateNotAllowedReason;
    }

    public static PluginConfUpdatable allowUpdate() {
        return new PluginConfUpdatable(true, null);
    }

    public static PluginConfUpdatable preventUpdate(String rejectionCause) {
        return new PluginConfUpdatable(false, rejectionCause);
    }

    public boolean isUpdateAllowed() {
        return updateAllowed;
    }

    public String getUpdateNotAllowedReason() {
        return updateNotAllowedReason;
    }

}
