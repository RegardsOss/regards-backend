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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.controllers;

import java.util.Set;
import java.util.TreeSet;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectUpdateElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;

/**
 * Cette classe permet de representer un element DATA_OBJECT_UPDATE qui peut etre cree par les differents process de
 * SIPAD-SSALTO un dataObjectUpdate se fait sur la liste de identifiants des objets de stockage uniquement
 * 
 * @author Christophe Mertz
 */

public class DataObjectUpdateElementControler extends DataObjectElementControler {

    /**
     * nom du bloc update
     */
    private static final String ENTITY_TYPE_VALUE = "DATA_OBJECT_UPDATE";

    /**
     * renvoie un bloc de type
     * 
     * <pre>
     * &lt;DATA_OBJECT_UPDATE_SSALTO&gt;
     *     &lt;DATA_OBJECT_IDENTIFIER&gt;   &lt;/DATA_OBJECT_IDENTIFIER&gt;
     *     &lt;DATA_STORAGE_OBJECT_IDENTIFIER&gt;   &lt;/DATA_STORAGE_OBJECT_IDENTIFIER&gt;
     *     &lt;DATA_STORAGE_OBJECT_IDENTIFIER&gt;   &lt;/DATA_STORAGE_OBJECT_IDENTIFIER&gt;
     *     <b>...</b>
     * &lt;/DATA_OBJECT_UPDATE_SSALTO&gt;
     * </pre>
     * 
     * @see ssalto.domain.data.descriptor.IDescriptorElement#getElement(DocumentImpl)
     */
    @Override
    public Element getElement(EntityDescriptorElement pEntityDescriptorElement, DocumentImpl pNewDoc) {
        Element doDescriptorElement = null;
        DataObjectUpdateElement dataObjectUpdateElement = (DataObjectUpdateElement) pEntityDescriptorElement;

        doDescriptorElement = pNewDoc.createElement("DATA_OBJECT_DESCRIPTION_SSALTO");
        doDescriptorElement.setAttribute(ENTITY_TYPE, ENTITY_TYPE_VALUE);
        
        buildUpdateElement(dataObjectUpdateElement, doDescriptorElement, pNewDoc);
        return doDescriptorElement;
    }

    /**
     * merge la liste des dataStorageIdentifiers
     */
    @Override
    public void merge(EntityDescriptorElement entityDescriptorElement, EntityDescriptorElement descrElement) {
        DataObjectUpdateElement dataObjectUpdateElement = (DataObjectUpdateElement) entityDescriptorElement;
        DataObjectUpdateElement descriptorElement = (DataObjectUpdateElement) descrElement;
        Set<String> dataStorageObjectIdentifiers = new TreeSet<>();

        dataStorageObjectIdentifiers.addAll(descriptorElement.getDataStorageObjectIdentifiers());
        dataObjectUpdateElement.setDataStorageObjectIdentifiers(dataStorageObjectIdentifiers);
    }

    /**
     * construit l'Element contenu dans le bloc update
     * 
     * @param doDescriptorElement
     *            le bloc update auquel attacher les elements
     * @param newDoc
     *            le DocumentImpl a utiliser pour creer les elements et les noeuds.
     */
    public void buildUpdateElement(DataObjectUpdateElement dataObjectUpdateElement, Element doDescriptorElement,
            DocumentImpl newDoc) {
        Element idElement = newDoc.createElement(DATA_OBJECT_IDENTIFIER);
        idElement.appendChild(newDoc.createTextNode(dataObjectUpdateElement.getDataObjectIdentifier()));
        doDescriptorElement.appendChild(idElement);
        buildDataStorageUpdateElement(dataObjectUpdateElement, doDescriptorElement, newDoc);
    }
}
