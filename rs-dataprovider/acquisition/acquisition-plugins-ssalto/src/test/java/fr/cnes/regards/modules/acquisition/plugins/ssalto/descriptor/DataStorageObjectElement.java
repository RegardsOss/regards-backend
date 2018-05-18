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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor;

import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;

/**
 * element descripteur pour un dataStorageObject. a partir des informations fournies par le {@link AcquisitionFile}
 * 
 * @author Christophe Mertz
 */

public abstract class DataStorageObjectElement extends EntityDescriptorElement {

    private String dataStorageObjectIdentifier;

    private Long fileSize;

    private String onlinePath;

    private String onlineFileName;

    private String offLinePath;

    private String offLineArchive;

    private String offLineFileName;

    /**
     * Default constructor
     */
    public DataStorageObjectElement() {
        super();
    }

    @Override
    public String getEntityId() {
        return dataStorageObjectIdentifier;
    }

    public void setDataStorageObjectIdentifier(String pDataStorageObjectIdentifier) {
        dataStorageObjectIdentifier = pDataStorageObjectIdentifier;
    }

    public String getDataStorageObjectIdentifier() {
        return dataStorageObjectIdentifier;
    }

    public void setFileSize(Long pFileSize) {
        fileSize = pFileSize;
    }

    public void setOffLineArchive(String pOffLineArchive) {
        offLineArchive = pOffLineArchive;
    }

    public void setOffLineFileName(String pOffLineFileName) {
        offLineFileName = pOffLineFileName;
    }

    public void setOffLinePath(String pOffLinePath) {
        offLinePath = pOffLinePath;
    }

    public void setOnlineFileName(String pOnlineFileName) {
        onlineFileName = pOnlineFileName;
    }

    public void setOnlinePath(String pOnlinePath) {
        onlinePath = pOnlinePath;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public String getOffLineArchive() {
        return offLineArchive;
    }

    public String getOffLineFileName() {
        return offLineFileName;
    }

    public String getOffLinePath() {
        return offLinePath;
    }

    public String getOnlineFileName() {
        return onlineFileName;
    }

    public String getOnlinePath() {
        return onlinePath;
    }

}
