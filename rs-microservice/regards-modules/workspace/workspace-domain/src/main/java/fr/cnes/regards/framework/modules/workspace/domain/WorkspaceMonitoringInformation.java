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
 *
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class WorkspaceMonitoringInformation {

    private static final String BYTES_UNIT = "B";

    private String storagePhysicalId;

    private String totalSpace;

    private String usedSpace;

    private String freeSpace;

    private Double occupationRatio;

    private String path;

    public WorkspaceMonitoringInformation(String storagePhysicalId, Long totalSpace, Long usedSpace,
            Long freeSpace, String path) {
        this.storagePhysicalId = storagePhysicalId;
        this.totalSpace = totalSpace + BYTES_UNIT;
        this.usedSpace = usedSpace + BYTES_UNIT;
        this.freeSpace = freeSpace + BYTES_UNIT;
        this.occupationRatio = Double.valueOf(usedSpace) / totalSpace;
        this.path = path;
    }

    public String getStoragePhysicalId() {
        return storagePhysicalId;
    }

    public void setStoragePhysicalId(String storagePhysicalId) {
        this.storagePhysicalId = storagePhysicalId;
    }

    public String getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(String totalSpace) {
        this.totalSpace = totalSpace;
    }

    public String getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(String usedSpace) {
        this.usedSpace = usedSpace;
    }

    public String getFreeSpace() {
        return freeSpace;
    }

    public void setFreeSpace(String freeSpace) {
        this.freeSpace = freeSpace;
    }

    public Double getOccupationRatio() {
        return occupationRatio;
    }

    public void setOccupationRatio(Double occupationRatio) {
        this.occupationRatio = occupationRatio;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
