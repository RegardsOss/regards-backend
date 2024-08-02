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
package fr.cnes.regards.modules.dam.domain.entities.metadata;

/**
 * Information about a group access to a specific dataset for data objects.
 *
 * @author Sébastien Binda
 */
public class DataObjectGroup {

    /**
     * Group name
     */
    private String groupName;

    /**
     * Does the group have access to data files ?
     */
    private Boolean dataFileAccess;

    /**
     * Does the groupe have access to the data objects ?
     */
    private Boolean dataObjectAccess;

    /**
     * Does the group have access to the dataset ?
     */
    private Boolean datasetAccess;

    /**
     * Does the group have access to the dataobjects metadatas ?
     */
    private String metaDataObjectAccessFilterPluginBusinessId;

    /**
     * Identifier of the plugin configuration used to define specific access to data objects metadatas.<br/>
     * Can be null, in this case all dataobjects of the dataset are available for the group.
     */
    private String dataObjectAccessFilterPluginBusinessId;

    /**
     * Needed for jsonIter deserialization
     */
    public DataObjectGroup() {
    }

    public DataObjectGroup(String groupName,
                           Boolean datasetAccess,
                           Boolean dataFileAccess,
                           Boolean dataObjectAccess,
                           String metaDataObjectAccessFilterPlugin,
                           String dataObjectAccessFilterPlugin) {
        super();
        this.groupName = groupName;
        this.dataFileAccess = dataFileAccess;
        this.dataObjectAccess = dataObjectAccess;
        this.datasetAccess = datasetAccess;
        this.metaDataObjectAccessFilterPluginBusinessId = metaDataObjectAccessFilterPlugin;
        this.dataObjectAccessFilterPluginBusinessId = dataObjectAccessFilterPlugin;
    }

    public String getGroupName() {
        return groupName;
    }

    public Boolean getDataFileAccess() {
        return dataFileAccess;
    }

    public String getDataObjectAccessFilterPluginBusinessId() {
        return dataObjectAccessFilterPluginBusinessId;
    }

    public Boolean getDatasetAccess() {
        return datasetAccess;
    }

    public String getMetaDataObjectAccessFilterPluginBusinessId() {
        return metaDataObjectAccessFilterPluginBusinessId;
    }

    public Boolean getDataObjectAccess() {
        return dataObjectAccess;
    }
}
