/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.acquisition.plugins.ISipGenerationPlugin;
import fr.cnes.regards.modules.ingest.dto.sip.SIPBuilder;

/**
 * Abstract class to provide a configurable {@link ISipGenerationPlugin} tool
 * to add storage information for files into SIPs.
 * @author Sébastien Binda
 */
public abstract class AbstractStorageInformation {

    public static final String STAF_STORAGE_NODE = "stafStorageNode";

    public static final String SIP_STAF_STORAGE_NODE_KEY = "staf_node";

    public static final String DATASET = "dataset";

    public static final String SIP_DATASET_KEY = "dataset";

    @PluginParameter(name = STAF_STORAGE_NODE, label = "STAF Storage node", optional = true)
    protected String stafStorageNode;

    @PluginParameter(name = DATASET, label = "Dataset name", optional = true)
    protected String dataset;

    /**
     * Add storage information read from the plugin parameter pluginIdStorageDirectoryMap
     * @param sipBuilder
     */
    protected void addStorageInfomation(SIPBuilder sipBuilder) {
        if (stafStorageNode != null) {
            sipBuilder.addDescriptiveInformation(SIP_STAF_STORAGE_NODE_KEY, stafStorageNode);
        }
        if (dataset != null) {
            sipBuilder.addDescriptiveInformation(SIP_DATASET_KEY, dataset);
        }
    }

}
