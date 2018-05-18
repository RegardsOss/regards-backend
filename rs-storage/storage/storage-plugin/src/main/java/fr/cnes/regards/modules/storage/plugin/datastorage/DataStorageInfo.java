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
package fr.cnes.regards.modules.storage.plugin.datastorage;

import org.hibernate.validator.constraints.NotEmpty;

import fr.cnes.regards.modules.storage.plugin.datastorage.validation.FileSize;

/**
 * Represents information on the current state of a data storage. <br/>
 * This will then be wrapped into a {@link PluginStorageInfo} which will have more informations on the plugin.
 *
 * @author Sylvain Vissiere-Guerinet
 */
public class DataStorageInfo {

    /**
     * The storage unit
     */
    public static final String BYTES_UNIT = "B";

    /**
     * Identifier of the data storage being monitored.
     */
    @NotEmpty
    private String storagePhysicalId;

    /**
     * This field contains the value and the unit. For example, 1234567925B ~= 1.177GiB ~= 1.235GB.
     */
    @FileSize
    private String totalSize;

    /**
     * This field contains the value and the unit. For example, 1234567925B ~= 1.177GiB ~= 1.235GB.
     */
    @FileSize
    private String usedSize;

    /**
     * Disk usage ratio in percent
     */
    private Double ratio;

    /**
     * Default constructor assuming that all numerical value given are expressed in Bytes.
     *
     * @param storagePhysicalId {@link DataStorageInfo#storagePhysicalId}
     * @param totalSize {@link DataStorageInfo#totalSize}
     * @param usedSize {@link DataStorageInfo#usedSize}
     */
    public DataStorageInfo(@NotEmpty String storagePhysicalId, long totalSize, long usedSize) {
        super();
        this.storagePhysicalId = storagePhysicalId;
        this.totalSize = totalSize + BYTES_UNIT;
        this.usedSize = usedSize + BYTES_UNIT;
        this.ratio = (Double.valueOf(usedSize) / totalSize) * 100;
    }

    /**
     * @return the storage physical id
     */
    public String getStoragePhysicalId() {
        return storagePhysicalId;
    }

    /**
     * Set the storage physical id
     * @param storagePhysicalId
     */
    public void setStoragePhysicalId(String storagePhysicalId) {
        this.storagePhysicalId = storagePhysicalId;
    }

    /**
     * @return the total size
     */
    public String getTotalSize() {
        return totalSize;
    }

    /**
     * Set the total size
     * @param pTotalSize
     */
    public void setTotalSize(@FileSize String pTotalSize) {
        totalSize = pTotalSize;
    }

    /**
     * @return the used size
     */
    public String getUsedSize() {
        return usedSize;
    }

    /**
     * Set the used size
     * @param pUsedSize
     */
    public void setUsedSize(@FileSize String pUsedSize) {
        usedSize = pUsedSize;
    }

    /**
     * @return the ratio
     */
    public Double getRatio() {
        return ratio;
    }

    /**
     * Set the ratio
     * @param ratio
     */
    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataStorageInfo that = (DataStorageInfo) o;

        return storagePhysicalId != null ?
                storagePhysicalId.equals(that.storagePhysicalId) :
                that.storagePhysicalId == null;
    }

    @Override
    public int hashCode() {
        return storagePhysicalId != null ? storagePhysicalId.hashCode() : 0;
    }
}
