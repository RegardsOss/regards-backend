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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor;

/**
 * element descripteur pour un dataStorageObject. a partir des informations fournies d'une part par le SsaltoFile
 * concerne et par le PhysicalFile du staf qui est traite si besoin
 * 
 * @author Christophe Mertz
 */

public abstract class DataStorageObjectElement extends EntityDescriptorElement {

    private String dataStorageObjectIdentifier_;

    private Long fileSize_;

    private String onlinePath_;

    private String onlineFileName_;

    private String offLinePath_;

    private String offLineArchive_;

    private String offLineFileName_;

    // TODO CMZ à confirmer
    //    private TransformerTypeEnum transformer_;

    /**
     * constructeur par defaut
     * 
     * @since 1.0
     * 
     */
    public DataStorageObjectElement() {
        super();
    }

    /**
     * renvoie le dataStorageObjectIdentifier Methode surchargee
     * 
     * @see ssalto.domain.data.descriptor.EntityDescriptorElement#getEntityId()
     * @since 1.0
     */
    @Override
    public String getEntityId() {
        return dataStorageObjectIdentifier_;
    }

    // GETTERS AND SETTERS

    public void setDataStorageObjectIdentifier(String pDataStorageObjectIdentifier) {
        dataStorageObjectIdentifier_ = pDataStorageObjectIdentifier;
    }

    public String getDataStorageObjectIdentifier() {
        return dataStorageObjectIdentifier_;
    }

    public void setFileSize(Long pFileSize) {
        fileSize_ = pFileSize;
    }

    public void setOffLineArchive(String pOffLineArchive) {
        offLineArchive_ = pOffLineArchive;
    }

    public void setOffLineFileName(String pOffLineFileName) {
        offLineFileName_ = pOffLineFileName;
    }

    public void setOffLinePath(String pOffLinePath) {
        offLinePath_ = pOffLinePath;
    }

    public void setOnlineFileName(String pOnlineFileName) {
        onlineFileName_ = pOnlineFileName;
    }

    public void setOnlinePath(String pOnlinePath) {
        onlinePath_ = pOnlinePath;
    }

    public Long getFileSize() {
        return fileSize_;
    }

    public String getOffLineArchive() {
        return offLineArchive_;
    }

    public String getOffLineFileName() {
        return offLineFileName_;
    }

    public String getOffLinePath() {
        return offLinePath_;
    }

    public String getOnlineFileName() {
        return onlineFileName_;
    }

    public String getOnlinePath() {
        return onlinePath_;
    }

    // TODO CMZ à confirmer
    //    public TransformerTypeEnum getTransformer() {
    //        return transformer_;
    //    }
    //
    //    public void setTransformer(String pTransformer) {
    //        transformer_ = TransformerTypeEnum.parse(pTransformer);
    //    }
    //
    //    public void setTransformer(TransformerTypeEnum pTransformer) {
    //        transformer_ = pTransformer;
    //    }
}
