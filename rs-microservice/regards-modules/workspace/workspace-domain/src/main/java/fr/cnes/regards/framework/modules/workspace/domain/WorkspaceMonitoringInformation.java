/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.workspace.domain;

/**
 * POJO containing monitoring information on the workspace
 *
 * @author Sylvain Vissiere-Guerinet
 */
public class WorkspaceMonitoringInformation {

    /**
     * The storage unit
     */
    private static final String BYTES_UNIT = "B";

    /**
     * Workspace storage physical id
     */
    private String storagePhysicalId;

    /**
     * Workspace total space
     */
    private String totalSpace;

    /**
     * Workspace used space
     */
    private String usedSpace;

    /**
     * Workspace free space
     */
    private String freeSpace;

    /**
     * Workspace occupation ratio
     */
    private Double occupationRatio;

    /**
     * Workspace path
     */
    private String path;

    /**
     * Contructor setting the given attributes from the parameters
     * @param storagePhysicalId
     * @param totalSpace
     * @param usedSpace
     * @param freeSpace
     * @param path
     */
    public WorkspaceMonitoringInformation(String storagePhysicalId, Long totalSpace, Long usedSpace,
            Long freeSpace, String path) {
        this.storagePhysicalId = storagePhysicalId;
        this.totalSpace = totalSpace + BYTES_UNIT;
        this.usedSpace = usedSpace + BYTES_UNIT;
        this.freeSpace = freeSpace + BYTES_UNIT;
        this.occupationRatio = Double.valueOf(usedSpace) / totalSpace;
        this.path = path;
    }

    /**
     * @return storage physical id
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
     * @return total space
     */
    public String getTotalSpace() {
        return totalSpace;
    }

    /**
     * Set the total space
     * @param totalSpace
     */
    public void setTotalSpace(String totalSpace) {
        this.totalSpace = totalSpace;
    }

    /**
     * @return the used space
     */
    public String getUsedSpace() {
        return usedSpace;
    }

    /**
     * Set the used space
     * @param usedSpace
     */
    public void setUsedSpace(String usedSpace) {
        this.usedSpace = usedSpace;
    }

    /**
     * @return the free space
     */
    public String getFreeSpace() {
        return freeSpace;
    }

    /**
     * Set the free space
     * @param freeSpace
     */
    public void setFreeSpace(String freeSpace) {
        this.freeSpace = freeSpace;
    }

    /**
     * @return the occupation ratio
     */
    public Double getOccupationRatio() {
        return occupationRatio;
    }

    /**
     * Set the occupation ratio
     * @param occupationRatio
     */
    public void setOccupationRatio(Double occupationRatio) {
        this.occupationRatio = occupationRatio;
    }

    /**
     * @return the path
     */
    public String getPath() {
        return path;
    }

    /**
     * Set the path
     * @param path
     */
    public void setPath(String path) {
        this.path = path;
    }
}
