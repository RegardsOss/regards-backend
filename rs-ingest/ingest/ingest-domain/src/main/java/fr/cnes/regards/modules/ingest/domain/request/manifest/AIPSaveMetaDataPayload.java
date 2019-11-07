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
package fr.cnes.regards.modules.ingest.domain.request.manifest;

/**
 * @author Léo Mieulet
 */
public class AIPSaveMetaDataPayload {

    private boolean removeCurrentMetaData;

    private boolean computeChecksum;

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

    public static AIPSaveMetaDataPayload build (boolean removeCurrentMetaData, boolean computeChecksum) {
        AIPSaveMetaDataPayload aipSaveMetaDataPayload = new AIPSaveMetaDataPayload();
        aipSaveMetaDataPayload.setComputeChecksum(computeChecksum);
        aipSaveMetaDataPayload.setRemoveCurrentMetaData(removeCurrentMetaData);
        return aipSaveMetaDataPayload;
    }
}
