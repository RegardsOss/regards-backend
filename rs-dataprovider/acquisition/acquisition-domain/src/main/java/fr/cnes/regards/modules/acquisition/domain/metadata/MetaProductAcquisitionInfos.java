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

/**
 * Cette classe regroupe les informations d'acquisisiton communes à la fourniture
 *  
 * @author Christophe Mertz
 */
public class MetaProductAcquisitionInfos {

    /**
     * indique la periodicite de depot des fichiers de la fourniture
     */
    private int periodicity;

    /**
     * indique si les meta donnees sont fournies avec les donnees lors de la fourniture
     */
    private Boolean metaDataSupplied;

    /**
     * le repertoire contenant les meta donnees si elles sont fournies avec les donnees
     */
    private String metaDataFolder;

    /**
     * indique si la description des fichiers du produit sont inclus dans le fichier de description du produit ou s'ils
     * sont a part
     */
    private Boolean fileMetaDataIncluded;

    /**
     * Default constructor
     */
    public MetaProductAcquisitionInfos() {
        super();
    }

    public Boolean isFileMetaDataIncluded() {
        return fileMetaDataIncluded;
    }

    public String getMetaDataFolder() {
        return metaDataFolder;
    }

    public Boolean isMetaDataSupplied() {
        return metaDataSupplied;
    }

    public int getPeriodicity() {
        return periodicity;
    }

    public void setFileMetaDataIncluded(Boolean pFileMetaDataIncluded) {
        fileMetaDataIncluded = pFileMetaDataIncluded;
    }

    public Boolean getFileMetaDataIncluded() {
        return fileMetaDataIncluded;
    }

    public void setMetaDataFolder(String pMetaDataFolder) {
        metaDataFolder = pMetaDataFolder;
    }

    public void setMetaDataSupplied(Boolean pMetaDataSupplied) {
        metaDataSupplied = pMetaDataSupplied;
    }

    public void setPeriodicity(int pPeriodicity) {
        periodicity = pPeriodicity;
    }
}
