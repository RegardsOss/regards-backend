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

import java.util.List;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDeletionDescriptorElement;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.EntityDescriptorElement;

/**
 * @author Christophe Mertz
 */

public class EntityDeletionDescriptorElementControler extends EntityDescriptorElementControler {

    /**
     * Elements XML pour les identifiant des elements a supprimer.
     */
    private static String XML_DELETE_OBJECT = "DELETE_OBJECT";

    private static String XML_DATA_OBJECT_IDENTIFIER = "DATA_OBJECT_IDENTIFIER";

    private static String XML_DATA_STORAGE_OBJECT_IDENTIFIER = "DATA_STORAGE_OBJECT_IDENTIFIER";

    @Override
    public Element getElement(EntityDescriptorElement entityDescriptorElement, DocumentImpl newDoc) {
        EntityDeletionDescriptorElement entityDeletionDescriptorElement = (EntityDeletionDescriptorElement) entityDescriptorElement;
        Element rootElement = newDoc.createElement(XML_DELETE_OBJECT);

        for (String doId : entityDeletionDescriptorElement.getDataObjectList()) {
            Element idElement = newDoc.createElement(XML_DATA_OBJECT_IDENTIFIER);
            idElement.appendChild(newDoc.createTextNode(doId));
            rootElement.appendChild(idElement);
        }
        for (String dsoId : entityDeletionDescriptorElement.getDataStorageObjectList()) {
            Element idElement = newDoc.createElement(XML_DATA_STORAGE_OBJECT_IDENTIFIER);
            idElement.appendChild(newDoc.createTextNode(dsoId));
            rootElement.appendChild(idElement);
        }
        return rootElement;
    }

    @Override
    public void merge(EntityDescriptorElement entityDescriptorElement, EntityDescriptorElement descrElement) {
        EntityDeletionDescriptorElement entityDeletionDescriptorElement = (EntityDeletionDescriptorElement) entityDescriptorElement;
        EntityDeletionDescriptorElement descriptorElement = (EntityDeletionDescriptorElement) descrElement;

        List<String> dataObjectList = entityDeletionDescriptorElement.getDataObjectList();
        List<String> dataStorageObjectList = entityDeletionDescriptorElement.getDataStorageObjectList();

        dataObjectList.addAll(descriptorElement.getDataObjectList());
        dataStorageObjectList.addAll(descriptorElement.getDataStorageObjectList());
    }
}
