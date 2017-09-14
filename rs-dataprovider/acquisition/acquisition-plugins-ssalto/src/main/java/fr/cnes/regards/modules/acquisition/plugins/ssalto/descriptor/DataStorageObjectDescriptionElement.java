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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((getDataStorageObjectIdentifier() == null) ? 0 : getDataStorageObjectIdentifier().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DataStorageObjectDescriptionElement other = (DataStorageObjectDescriptionElement) obj;
        if (getDataStorageObjectIdentifier() == null) {
            if (other.getDataStorageObjectIdentifier() != null)
                return false;
        } else if (!getDataStorageObjectIdentifier().equals(other.getDataStorageObjectIdentifier()))
            return false;
        return true;
    }
}
