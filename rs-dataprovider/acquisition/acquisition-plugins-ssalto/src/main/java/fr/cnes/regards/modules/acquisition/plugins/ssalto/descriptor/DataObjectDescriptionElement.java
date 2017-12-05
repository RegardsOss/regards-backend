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

import java.util.ArrayList;
import java.util.List;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;

/**
 * Cette classe permet de representer un element DATA_OBJECT_DESCRIPTION, de le parser pour pouvoir le reintegrer dans
 * le fichier global. Nous n'avons pas besoin de connaitre le contenu du bloc descripteur
 * 
 * @author Christophe Mertz
 */

public class DataObjectDescriptionElement extends DataObjectElement {

    protected List<Attribute> attributeLst = null;

    private String ascendingNode = null;

    private String startDate = null;

    private String stopDate = null;

    private String cycleNumber = null;

    private String longitudeMin = null;

    private String longitudeMax = null;

    private String latitudeMin = null;

    private String latitudeMax = null;

    private String fileCreationDate = null;

    private String objectVersion = null;

    private String fileSize = null;

    /**
     * constructeur par defaut
     * 
     */
    public DataObjectDescriptionElement() {
        super();
        attributeLst = new ArrayList<>();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((getDataObjectIdentifier() == null) ? 0 : getDataObjectIdentifier().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DataObjectDescriptionElement other = (DataObjectDescriptionElement) obj;
        if (getDataObjectIdentifier() == null) {
            if (other.getDataObjectIdentifier() != null) {
                return false;
            }
        } else if (!getDataObjectIdentifier().equals(other.getDataObjectIdentifier())) {
            return false;
        }
        return true;
    }

    /**
     * Cette methode permet d'ajouter un attribut a la liste d'attributs non standard de l'entite.
     * 
     * @param attribute
     *            l'attribut a ajouter
     */
    public void addAttribute(Attribute attribute) {

        attributeLst.add(attribute);
    }

    /**
     * 
     * renvoie DESC_ELEMENT_TYPE
     */
    @Override
    public ElementType getElementType() {
        return ElementType.DESC_ELEMENT_TYPE;
    }

    /**
     * renvoie un ordre pour pouvoir etre trie dans les fichiers descripteurs
     */
    @Override
    protected int getOrder() {
        return 2;
    }

    public String getAscendingNode() {
        return ascendingNode;
    }

    public void setAscendingNode(String ascNode) {
        ascendingNode = ascNode;
    }

    protected void setAttributeLst_(List<Attribute> attrList) {
        this.attributeLst = attrList;
    }

    public void setStartDate(String newStartDate) {
        this.startDate = newStartDate;
    }

    public void setStopDate(String newStopDate) {
        this.stopDate = newStopDate;
    }

    public void setCycleNumber(String newCycleNumber) {
        this.cycleNumber = newCycleNumber;
    }

    public void setLongitudeMin(String newLongitudeMin) {
        this.longitudeMin = newLongitudeMin;
    }

    public void setLongitudeMax(String newLongitudeMax) {
        this.longitudeMax = newLongitudeMax;
    }

    public void setLatitudeMin(String newLatitudeMin) {
        this.latitudeMin = newLatitudeMin;
    }

    public void setLatitudeMax(String newLatitudeMax) {
        this.latitudeMax = newLatitudeMax;
    }

    public void setFileCreationDate(String newFileCreationDate) {
        this.fileCreationDate = newFileCreationDate;
    }

    public void setObjectVersion(String newObjectVersion) {
        this.objectVersion = newObjectVersion;
    }

    public String getFileSize() {
        return fileSize;
    }

    public void setFileSize(String newFileSize) {
        fileSize = newFileSize;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getStopDate() {
        return stopDate;
    }

    public String getCycleNumber() {
        return cycleNumber;
    }

    public String getLongitudeMin() {
        return longitudeMin;
    }

    public String getLongitudeMax() {
        return longitudeMax;
    }

    public String getLatitudeMin() {
        return latitudeMin;
    }

    public String getLatitudeMax() {
        return latitudeMax;
    }

    public String getFileCreationDate() {
        return fileCreationDate;
    }

    public String getObjectVersion() {
        return objectVersion;
    }

    public List<Attribute> getAttributeLst() {
        return attributeLst;
    }

    public void setAttributeLst(List<Attribute> pAttributeLst) {
        attributeLst = pAttributeLst;
    }

}
