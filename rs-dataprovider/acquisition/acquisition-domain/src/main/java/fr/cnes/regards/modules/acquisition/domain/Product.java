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
package fr.cnes.regards.modules.acquisition.domain;

import java.util.HashSet;
import java.util.Set;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class Product {

    /**
     * Prefixe d'identification des fichiers descripteurs de fichier
     */
    public static final String IDENT_PRODUCT_PREFIX = "PRODUCT_";

    /**
     * status du produit
     */
    private ProductStatus status_;

    /**
     * nom du produit
     */
    private String productName;

    /**
     * identifiant interne du produit<br>
     * null si le produit n'existe pas en base
     */
    private Integer productId = null;

    /**
     * type de produit
     */
    private MetaProduct metaProduct;

    /**
     * numero de version du produit
     */
    private int version;

    //    /**
    //     * nom du fichier de meta donnee du produit
    //     */
    //    private DescriptorFile metaDataFileName_;

    /**
     * Liste des fichiers composant les produit
     */
    private Set<AcquisitionFile> fileList = new HashSet<>();

    /**
     * Default constructor
     */
    public Product() {
    }

    /**
     * @return Boolean
     */
    public Boolean isStoreInStaff() {
        return null;
    }

    /**
     * Ajoute un fichier a la liste.
     * 
     * @param pFile
     *            : un fichier
     * @since 1.0
     */
    public void addFileToProduct(AcquisitionFile pFile) {

        fileList.add(pFile);
    }

    /**
     * 
     * Methode surchargee<br>
     * Deux produits ayant des identifiants identiques sont consideres comme egaux.
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     * @since 1.0
     */
    @Override
    public boolean equals(Object obj) {
        Product aProduct = (Product) obj;
        return productId.equals(aProduct.getProductId());
    }

    public Set<AcquisitionFile> getAcquisitionFile() {
        return fileList;
    }

    public void addAcquisitionFile(AcquisitionFile acquisitionFile) {
        fileList.add(acquisitionFile);
    }

    public void setAcquisitionFile(Set<AcquisitionFile> acquisitionFiles) {
        fileList = acquisitionFiles;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public String getProductName() {
        return productName;
    }

    public ProductStatus getStatus() {
        return status_;
    }

    public int getVersion() {
        return version;
    }

    public void setMetaProduct(MetaProduct pMetaProduct) {
        metaProduct = pMetaProduct;
    }

    public void setProductName(String pProductName) {
        productName = pProductName;
    }

    public void setStatus(ProductStatus pStatus) {
        status_ = pStatus;
    }

    public void setVersion(int pVersion) {
        version = pVersion;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer pProductId) {
        productId = pProductId;
    }
}