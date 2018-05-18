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
package fr.cnes.regards.modules.storage.plugin.allocation.strategy;

/**
 * Plugin parameter POJO for plugin {@link AIPMiscAllocationStrategyPlugin}
 * @author Sébastien Binda
 */
public class AIPMiscStorageInformation {

    /**
     * Plugin identifier
     */
    private String pluginId;

    /**
     * Disrectory to use
     */
    private String directory;

    public String getPluginId() {
        return pluginId;
    }

    public String getDirectory() {
        return directory;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

}
