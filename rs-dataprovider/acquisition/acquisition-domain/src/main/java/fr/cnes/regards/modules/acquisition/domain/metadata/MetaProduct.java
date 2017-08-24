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
package fr.cnes.regards.modules.acquisition.domain.metadata;

import java.util.List;



/**
 * This class reprensents a product type
 * 
 * @author Christophe Mertz
 *
 */
public class MetaProduct {

    /**
     * le nom du type de produit (maximum 64 characteres)
     */
    private String productTypeName;

    /**
     * La liste des type de fichiers composant ce produit (liste de MetaFile)
     */
    private List<MetaFile> metaFileList;

    /**
     * Les informations d'acquisition pour ce type de produit
     */
    private MetaProductAcquisitionInfos acquisitionInformations;

    //    /**
    //     * La fourniture a laquelle est rattache ce metaproduct.
    //     */
    //    private Supply supply_;

    /**
     * Default constructor
     */
    public MetaProduct() {
        super();
    }

    public MetaProductAcquisitionInfos getAcquisitionInformations() {
        return acquisitionInformations;
    }

    public List<MetaFile> getMetaFileList() {
        return metaFileList;
    }

    public String getProductTypeName() {
        return productTypeName;
    }

    public void setAcquisitionInformations(MetaProductAcquisitionInfos pAcquisitionInformations) {
        acquisitionInformations = pAcquisitionInformations;
//        // FIXME etudier autre possibilite d'initialisation
//        if (acquisitionInformations.getMetaDataCreationPlugin() != null) {
//            acquisitionInformations.getMetaDataCreationPlugin().setMProduct(this);
//        }
    }

    public void setMetaFileList(List<MetaFile> pMetaFileList) {
        metaFileList = pMetaFileList;
        // for digester
        updateMetafileMetaproduct();
    }

    public void setProductTypeName(String pProductTypeName) {
        productTypeName = pProductTypeName;
    }

//    public Supply getSupply() {
//        return supply_;
//    }
//
//    public void setSupply(Supply pSupply) {
//        supply_ = pSupply;
//    }

    /**
     * Utilise lors de la creation par le digester. Vu les references cyclique le digester ne peut pas initialise le
     * metaproduct des metafiles.
     * 
     * @since 1.0
     */
    private void updateMetafileMetaproduct() {
        for (MetaFile metafile : metaFileList) {
            metafile.setMetaProduct(this);

        }
    }
}
