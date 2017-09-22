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

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Element;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.descriptor.DataObjectElement;



/**
 * Cette classe est la classe mere des elements concernant les dataObject Elle contient des methodes pour generer les
 * blocs xml communs au update et description
 * 
 * @author Christophe Mertz
 */

public abstract class DataObjectElementControler extends EntityDescriptorElementControler {

    protected static final String DATA_OBJECT_IDENTIFIER = "DATA_OBJECT_IDENTIFIER";

    protected static final String ASCENDING_NODE = "ASCENDING_NODE";

    protected static final String DISPLAY_ORDER = "DISPLAY_ORDER";

    protected static final String TIME_PERIOD = "TIME_PERIOD";

    protected static final String START_DATE = "START_DATE";

    protected static final String STOP_DATE = "STOP_DATE";

    protected static final String CYCLE_NUMBER = "CYCLE_NUMBER";

    protected static final String ORBIT_NUMBER = "ORBIT_NUMBER";

    protected static final String GEO_COORDINATES = "GEO_COORDINATES";

    protected static final String LONGITUDE_MIN = "LONGITUDE_MIN";

    protected static final String LONGITUDE_MAX = "LONGITUDE_MAX";

    protected static final String LATITUDE_MIN = "LATITUDE_MIN";

    protected static final String LATITUDE_MAX = "LATITUDE_MAX";

    protected static final String RADICAL = "RADICAL";

    protected static final String FILE_CREATION_DATE = "FILE_CREATION_DATE";

    protected static final String OBJECT_VERSION = "OBJECT_VERSION";

    protected static final String FILE_SIZE = "FILE_SIZE";

    protected static final String DATA_STORAGE_OBJECT_IDENTIFIER = "DATA_STORAGE_OBJECT_IDENTIFIER";

    /**
     * construit les elements de la liste des dataStorageObjectIdentifier
     * 
     * @param doDescriptorElement
     * @param newDoc
     */
    protected void buildDataStorageUpdateElement(DataObjectElement dataObjectElement, Element doDescriptorElement,
            DocumentImpl newDoc) {
        if (dataObjectElement.getDataStorageObjectIdentifiers() != null) {
            for (String dsoId : dataObjectElement.getDataStorageObjectIdentifiers()) {
                Element dsoIdElement = newDoc.createElement(DATA_STORAGE_OBJECT_IDENTIFIER);
                dsoIdElement.appendChild(newDoc.createTextNode(dsoId));
                doDescriptorElement.appendChild(dsoIdElement);
            }
        }
    }
}
