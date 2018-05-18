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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;

/**
 * Abstract class to provide a configurable {@link ISipGenerationPlugin} tool
 * to add storage information for files into the miscInformation section.
 * @author Sébastien Binda
 */
public abstract class AbstractMiscStorageInformation {

    public static final String PLUGINID_STORAGEDIR_MAP = "pluginIdStorageDirectoryMap";

    /**
     * SIP misc section key for storage informations
     */
    public static final String SIP_MISC_STORAGE_KEY = "storage";

    @PluginParameter(name = PLUGINID_STORAGEDIR_MAP, label = "Define storage directory in archives per storage plugin",
            keylabel = "Identifier of DataStoragePlugin",
            description = "This parameter allows to fix for one or many storage plugin(s) the internal archive directory where files will be stored."
                    + " The pluginId is the identifier of the DataStoragePlugin type like \"Local\". The available DataStoragePlugin list is"
                    + " displayed when you create a new storage configuration.",
            optional = true)
    protected Map<String, String> pluginIdStorageDirectoryMap = Maps.newHashMap();

    /**
     * Add storage information read from the plugin parameter pluginIdStorageDirectoryMap
     * @param sipBuilder
     */
    protected void addMiscStorageInfomation(SIPBuilder sipBuilder) {
        List<MiscStorageInformation> infos = Lists.newArrayList();
        pluginIdStorageDirectoryMap
                .forEach((pluginId, directory) -> infos.add(new MiscStorageInformation(pluginId, directory)));
        sipBuilder.addMiscInformation(SIP_MISC_STORAGE_KEY, infos);
    }

}
