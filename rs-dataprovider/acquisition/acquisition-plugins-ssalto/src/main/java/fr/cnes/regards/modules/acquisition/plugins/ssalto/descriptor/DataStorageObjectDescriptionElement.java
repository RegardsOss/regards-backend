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
 * Cette classe permet de representer un Element de type DATA_STORAGE_OBJECT_DESCRIPTION
 * 
 * @author Christophe Mertz
 */

public class DataStorageObjectDescriptionElement extends DataStorageObjectElement {

    /**
     * constructeur par defaut pour le digester
     */
    public DataStorageObjectDescriptionElement() {
        super();
    }

    /**
     * renvoie true si pObj est de type DataStorageObjectDescriptionElement et que les dataStorageObjectIdentifier sont
     * identiques
     */
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof DataStorageObjectDescriptionElement) {
            DataStorageObjectDescriptionElement obj1 = (DataStorageObjectDescriptionElement) obj;
            result = getDataStorageObjectIdentifier().equals(obj1.getDataStorageObjectIdentifier());
        }
        return result;
    }

    /**
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
        return 1;
    }
}
