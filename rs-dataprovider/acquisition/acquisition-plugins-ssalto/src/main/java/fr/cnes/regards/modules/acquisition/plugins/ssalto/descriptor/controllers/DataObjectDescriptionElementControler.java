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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import java.util.ArrayList;
import java.util.List;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectDescriptionElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;

/**
 * Cette classe permet de representer un element DATA_OBJECT_DESCRIPTION, de le parser pour pouvoir le reintegrer dans
 * le fichier global. Nous n'avons pas besoin de connaitre le contenu du bloc descripteur
 * 
 * @author Christophe Mertz
 */

public class DataObjectDescriptionElementControler extends DataObjectElementControler {

    public DataObjectDescriptionElementControler() {
    }

    /**
     * renvoie le bloc DATA_OBJECT_DESCRIPTION_SSALTO a inserer dans un fichier descripteur
     */
    @Override
    public Element getElement(EntityDescriptorElement pEntityDescriptorElement, DocumentImpl pNewDoc) {
        DataObjectDescriptionElement dataObjectDescriptionElement = (DataObjectDescriptionElement) pEntityDescriptorElement;
        Element doDescriptorElement = null;

        //        try {
        doDescriptorElement = pNewDoc.createElement("DATA_OBJECT_DESCRIPTION_SSALTO");
        // TODO CMZ à confirmer DATA_OBJECT_DESCRIPTION_SSALTO ne devrait pas être en dur
        //                .createElement(DescConfiguration.getInstance().getProperties().getDataObjectDescriptionNode());
        doDescriptorElement.setAttribute(ENTITY_TYPE, "DATA_OBJECT_DESCRIPTION");
        //                .setAttribute(ENTITY_TYPE,
        //                              DescConfiguration.getInstance().getProperties().getDataObjectDescriptionType());
        Element identifierElement = pNewDoc.createElement(DATA_OBJECT_IDENTIFIER);
        identifierElement.appendChild(pNewDoc.createTextNode(dataObjectDescriptionElement.getDataObjectIdentifier()));
        doDescriptorElement.appendChild(identifierElement);
        buildElement(dataObjectDescriptionElement, doDescriptorElement, pNewDoc);
        //        }
        //        catch (SsaltoDomainException e) {
        //            LOGGER.error(e.getMessage());
        //        }

        return doDescriptorElement;
    }

    /**
     * contruit l'Element interne du bloc DATA_OBJECT_DESCRIPTION_SSALTO
     * 
     * @param pDataObjectElement
     * @param pNewDoc
     */
    protected void buildElement(DataObjectDescriptionElement pDataObjectDescriptionElement, Element pDataObjectElement,
            DocumentImpl pNewDoc) {
        buildOtherElement(pDataObjectDescriptionElement, pDataObjectElement, pNewDoc);
        for (Attribute attribute : pDataObjectDescriptionElement.getAttributeLst()) {
            if (attribute instanceof CompositeAttribute) {
                CompositeAttribute newAttribute = (CompositeAttribute) attribute;
                CompositeAttributeControler.createAttributeElement(newAttribute, pDataObjectElement, pNewDoc);
            } else {
                buildAttributeElement(pDataObjectElement, attribute, pNewDoc);
            }
        }
        buildFileSizeElement(pDataObjectDescriptionElement, pDataObjectElement, pNewDoc);
        buildDataStorageUpdateElement(pDataObjectDescriptionElement, pDataObjectElement, pNewDoc);
    }

    /**
     * construit le bloc de l'attribute fileSize
     */
    private void buildFileSizeElement(DataObjectDescriptionElement pDataObjectDescriptionElement,
            Element pDataObjectElement, DocumentImpl pNewDoc) {
        if (pDataObjectDescriptionElement.getFileSize() != null) {
            Element newElement = pNewDoc.createElement(FILE_SIZE);
            newElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getFileSize()));
            pDataObjectElement.appendChild(newElement);
        }
    }

    /**
     * construit un element representant un Attribute
     * 
     * @param element
     * @param pAttribute
     * @param pNewDoc
     */
    protected void buildAttributeElement(Element element, Attribute pAttribute, DocumentImpl pNewDoc) {
        for (Object value : pAttribute.getValueList()) {
            Element attElement = pNewDoc.createElement(pAttribute.getMetaAttribute().getName());
            attElement.appendChild(pNewDoc
                    .createTextNode(SsaltoControlers.getControler(pAttribute).doGetStringValue(value)));
            element.appendChild(attElement);
        }
    }

    /**
     * remplace l'ancien descripteur par le nouveau
     */
    @Override
    public void merge(EntityDescriptorElement pEntityDescriptorElement, EntityDescriptorElement pDescriptorElement) {
        DataObjectDescriptionElement dataObjectDescriptionElement = (DataObjectDescriptionElement) pEntityDescriptorElement;
        DataObjectDescriptionElement myElement = (DataObjectDescriptionElement) pDescriptorElement;
        List<Attribute> attributeLst = new ArrayList<>();

        attributeLst.addAll(myElement.getAttributeLst());
        dataObjectDescriptionElement.setAttributeLst(attributeLst);

        dataObjectDescriptionElement.setAscendingNode(myElement.getAscendingNode());
        dataObjectDescriptionElement.setDataObjectIdentifier(myElement.getDataObjectIdentifier());
        dataObjectDescriptionElement.setDataStorageObjectIdentifiers(myElement.getDataStorageObjectIdentifiers());
    }

    /**
     * Methode utilisee pour le plugin niveau produit
     * 
     * @param pDoDescriptorElement
     * @param pNewDoc
     */
    private void buildOtherElement(DataObjectDescriptionElement pDataObjectDescriptionElement,
            Element pDoDescriptorElement, DocumentImpl pNewDoc) {

        Element newElement;
        Element subElement;
        if (pDataObjectDescriptionElement.getAscendingNode() != null) {
            newElement = pNewDoc.createElement(ASCENDING_NODE);
            newElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getAscendingNode()));
            pDoDescriptorElement.appendChild(newElement);
        }
        if ((pDataObjectDescriptionElement.getStartDate() != null)
                && (pDataObjectDescriptionElement.getStopDate() != null)) {
            newElement = pNewDoc.createElement(TIME_PERIOD);
            subElement = pNewDoc.createElement(START_DATE);
            subElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getStartDate()));
            newElement.appendChild(subElement);
            subElement = pNewDoc.createElement(STOP_DATE);
            subElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getStopDate()));
            newElement.appendChild(subElement);
            pDoDescriptorElement.appendChild(newElement);
        }
        if (pDataObjectDescriptionElement.getCycleNumber() != null) {
            newElement = pNewDoc.createElement(CYCLE_NUMBER);
            newElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getCycleNumber()));
            pDoDescriptorElement.appendChild(newElement);
        }
        if ((pDataObjectDescriptionElement.getLongitudeMin() != null)
                && (pDataObjectDescriptionElement.getLongitudeMax() != null)
                && (pDataObjectDescriptionElement.getLatitudeMax() != null)
                && (pDataObjectDescriptionElement.getLatitudeMin() != null)) {
            newElement = pNewDoc.createElement(GEO_COORDINATES);
            subElement = pNewDoc.createElement(LONGITUDE_MIN);
            subElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getLongitudeMin()));
            newElement.appendChild(subElement);
            subElement = pNewDoc.createElement(LONGITUDE_MAX);
            subElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getLongitudeMax()));
            newElement.appendChild(subElement);
            subElement = pNewDoc.createElement(LATITUDE_MIN);
            subElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getLatitudeMin()));
            newElement.appendChild(subElement);
            subElement = pNewDoc.createElement(LATITUDE_MAX);
            subElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getLatitudeMax()));
            newElement.appendChild(subElement);
            pDoDescriptorElement.appendChild(newElement);
        }
        if (pDataObjectDescriptionElement.getFileCreationDate() != null) {
            newElement = pNewDoc.createElement(FILE_CREATION_DATE);
            newElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getFileCreationDate()));
            pDoDescriptorElement.appendChild(newElement);
        }
        if (pDataObjectDescriptionElement.getObjectVersion() != null) {
            newElement = pNewDoc.createElement(OBJECT_VERSION);
            newElement.appendChild(pNewDoc.createTextNode(pDataObjectDescriptionElement.getObjectVersion()));
            pDoDescriptorElement.appendChild(newElement);
        }
    }
}
