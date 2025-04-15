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

import java.util.Objects;
import java.util.Optional;

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
     * Identifier of the plugin configuration used to define specific access to data objects metadatas.<br/>
     * Can be null, in this case all dataobjects of the dataset are available for the group.
     */
    private String metaDataObjectAccessFilterPluginBusinessId;

    /**
     * Checksum with all IPluginParam parameters used to filter data objects.<br/>
     */
    private String metaDataObjectAccessFilterPluginParamsChecksum;

    /**
     * Needed for jsonIter deserialization
     */
    public DataObjectGroup() {
    }

    public DataObjectGroup(String groupName,
                           Boolean datasetAccess,
                           Boolean dataFileAccess,
                           Boolean dataObjectAccess,
                           String metaDataObjectAccessFilterPluginBusinessId,
                           String metaDataObjectAccessFilterPluginParamsChecksum) {
        super();
        this.groupName = groupName;
        this.dataFileAccess = dataFileAccess;
        this.dataObjectAccess = dataObjectAccess;
        this.datasetAccess = datasetAccess;
        this.metaDataObjectAccessFilterPluginBusinessId = metaDataObjectAccessFilterPluginBusinessId;
        this.metaDataObjectAccessFilterPluginParamsChecksum = metaDataObjectAccessFilterPluginParamsChecksum;
    }

    public String getGroupName() {
        return groupName;
    }

    public Boolean getDataFileAccess() {
        return dataFileAccess;
    }


    public String getMetaDataObjectAccessFilterPluginParamsChecksum() {
        return metaDataObjectAccessFilterPluginParamsChecksum;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataObjectGroup that)) {
            return false;
        }
        // NOTICE : dataObjectAccessFilterPluginBusinessId can be an empty String in ES and NULL from frontend.
        return Objects.equals(groupName, that.groupName)
               && Objects.equals(dataFileAccess, that.dataFileAccess)
               && Objects.equals(dataObjectAccess, that.dataObjectAccess)
               && Objects.equals(datasetAccess, that.datasetAccess)
               && Objects.equals(Optional.ofNullable(metaDataObjectAccessFilterPluginBusinessId).orElse(""),
                                 Optional.ofNullable(that.metaDataObjectAccessFilterPluginBusinessId).orElse(""))
               && Objects.equals(Optional.ofNullable(metaDataObjectAccessFilterPluginParamsChecksum).orElse(""),
                                 Optional.ofNullable(that.metaDataObjectAccessFilterPluginParamsChecksum).orElse(""));
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName,
                            dataFileAccess,
                            dataObjectAccess,
                            datasetAccess,
                            Optional.ofNullable(metaDataObjectAccessFilterPluginBusinessId).orElse(""),
                            Optional.ofNullable(metaDataObjectAccessFilterPluginParamsChecksum).orElse(""));
    }
}
