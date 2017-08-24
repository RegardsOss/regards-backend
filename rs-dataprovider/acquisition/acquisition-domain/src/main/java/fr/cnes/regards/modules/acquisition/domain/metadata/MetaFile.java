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

import javax.persistence.Entity;

/**
 * This class represents a file type.
 * 
 * @author Christophe Mertz
 *
 */
@Entity
public class MetaFile {

    /**
     * indique si c'est un fichier obligatoire (true)
     */
    private Boolean mandatory;

    /**
     * identifiant interne du type de fichier
     */
    private Integer metaFileId;

    /**
     * pattern permettant d'identifier les fichiers de ce type de fichier. (max 100 chars)
     */
    private String fileNamePattern;

    /**
     * La liste des repertoire à scanner pour ce type de fichiers (max 250 chars)
     */
    private SupplyDirectory[] scanDirectories;

    /**
     * nom du type de fichier (max chars 100)
     */
    private String name_;

    /**
     * repertoire dans lequel on depose les fichiers invalides pour ce type de fichiers
     */
    private String invalidFolder;

    /**
     * Le metaProduct auquel il est lié
     */
    private MetaProduct metaProduct;

    /**
     * une chaine de caractere informative sur le type( extension surtour) des fichiers represente par ce MetaFile (
     * exemple image, jpeg, ascii, binaire, exec...)
     */
    private String fileType;

    /**
     * commentaire ajoutes lors de la definition de la fourniture
     */
    private String comments;

    /**
     * Version
     */
    private Integer version;

    /**
     * Default constructor
     */
    public MetaFile() {
        super();
    }

    /**
     * Recupere le repertoire de scan correspondant au type de fichier passe en parametre.
     * 
     * @param pMFileId
     * @return
     */
    public SupplyDirectory getSupplyDirectory(Integer pMSupplyDirId) {
        SupplyDirectory supplyDir = null;
        if (pMSupplyDirId != null) {
            for (SupplyDirectory element : scanDirectories) {
                if (pMSupplyDirId.equals(element.getMSupplyDirId())) {
                    supplyDir = element;
                    break;
                }
            }
        }
        return supplyDir;
    }

    /**
     * Compare 2 types de fichier
     * 
     * @param pMetaFile
     * @return
     */
    public boolean equals(Object pObject) {
        boolean result = false;
        if (pObject != null) {
            result = metaFileId.intValue() == ((MetaFile) pObject).getMetaFileId().intValue();
        }
        return result;
    }

    public int hashCode() {
        return metaFileId.hashCode();
    }

    /**
     * 
     * Methode surchargee afin d'eviter d'utiliser la methode par defaut qui utilise la methode hashCode qui peut lever
     * une exception en cas de metaFileId_ null.<br>
     * Exemple d'utilisation : requete addSupply avec commons.Digester en DEBUG
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return fileNamePattern;
    }

    // GETTERS AND SETTERS
    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public String getInvalidFolder() {
        return invalidFolder;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public Integer getMetaFileId() {
        return metaFileId;
    }

    public MetaProduct getMetaProduct() {
        return metaProduct;
    }

    public String getName() {
        return name_;
    }

    public SupplyDirectory[] getScanDirectories() {
        return scanDirectories;
    }

    public void setFileNamePattern(String pFileNamePattern) {
        fileNamePattern = pFileNamePattern;
    }

    public void setInvalidFolder(String pInvalidFolder) {
        invalidFolder = pInvalidFolder;
    }

    public void setMandatory(Boolean pMandatory) {
        mandatory = pMandatory;
    }

    public void setMetaFileId(Integer pMetaFileId) {
        metaFileId = pMetaFileId;
    }

    public void setMetaProduct(MetaProduct pMetaProduct) {
        metaProduct = pMetaProduct;
    }

    public void setName(String pName) {
        name_ = pName;
    }

    public void setScanDirectories(SupplyDirectory[] pScanDirectories) {
        scanDirectories = pScanDirectories;
    }

    public String getComments() {
        return comments;
    }

    public String getFileType() {
        return fileType;
    }

    public void setComments(String pComments) {
        comments = pComments;
    }

    public void setFileType(String pFileType) {
        fileType = pFileType;
    }

    public void setSupplyDirList(List<SupplyDirectory> pSupplyDirList) {
        int i = 0;
        if (pSupplyDirList != null) {
            scanDirectories = new SupplyDirectory[pSupplyDirList.size()];
            for (SupplyDirectory supplyDirectory : pSupplyDirList) {
                scanDirectories[i] = supplyDirectory;
                i++;
            }
        }
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer pVersion) {
        version = pVersion;
    }
}