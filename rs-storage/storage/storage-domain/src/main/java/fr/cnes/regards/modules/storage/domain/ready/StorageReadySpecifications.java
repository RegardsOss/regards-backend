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
package fr.cnes.regards.modules.storage.domain.ready;

import java.util.List;

import fr.cnes.regards.modules.storage.domain.plugin.IDataStorage;

/**
 * Specifications for ready endpoint of storage microservice.
 * @author Sébastien Binda
 *
 */
public class StorageReadySpecifications {

    /**
     * Current active allocation strategy plugin configuration label
     */
    private String allocationStrategy;

    /**
     * Current actives {@link IDataStorage} plugin configuration labels
     */
    private List<String> dataStorages;

    public StorageReadySpecifications(String allocationStrategy, List<String> dataStorages) {
        super();
        this.allocationStrategy = allocationStrategy;
        this.dataStorages = dataStorages;
    }

    public String getAllocationStrategy() {
        return allocationStrategy;
    }

    public void setAllocationStrategy(String allocationStrategy) {
        this.allocationStrategy = allocationStrategy;
    }

    public List<String> getDataStorages() {
        return dataStorages;
    }

    public void setDataStorages(List<String> dataStorages) {
        this.dataStorages = dataStorages;
    }

}
