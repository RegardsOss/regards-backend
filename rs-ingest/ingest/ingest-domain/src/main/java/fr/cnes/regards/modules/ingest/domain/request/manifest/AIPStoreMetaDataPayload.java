/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.request.manifest;

import java.util.Set;

/**
 * @author Léo Mieulet
 */
public class AIPStoreMetaDataPayload {

    private boolean removeCurrentMetaData;

    private boolean computeChecksum;

    private Set<StoreLocation> storeLocations;


    public boolean isRemoveCurrentMetaData() {
        return removeCurrentMetaData;
    }

    public void setRemoveCurrentMetaData(boolean removeCurrentMetaData) {
        this.removeCurrentMetaData = removeCurrentMetaData;
    }

    public boolean isComputeChecksum() {
        return computeChecksum;
    }

    public void setComputeChecksum(boolean computeChecksum) {
        this.computeChecksum = computeChecksum;
    }

    public Set<StoreLocation> getStoreLocations() {
        return storeLocations;
    }

    public void setStoreLocations(Set<StoreLocation> storeLocations) {
        this.storeLocations = storeLocations;
    }

    public static AIPStoreMetaDataPayload build(Set<StoreLocation> storeLocations, boolean removeCurrentMetaData, boolean computeChecksum) {
        AIPStoreMetaDataPayload aipStoreMetaDataPayload = new AIPStoreMetaDataPayload();
        aipStoreMetaDataPayload.setStoreLocations(storeLocations);
        aipStoreMetaDataPayload.setComputeChecksum(computeChecksum);
        aipStoreMetaDataPayload.setRemoveCurrentMetaData(removeCurrentMetaData);
        return aipStoreMetaDataPayload;
    }
}
