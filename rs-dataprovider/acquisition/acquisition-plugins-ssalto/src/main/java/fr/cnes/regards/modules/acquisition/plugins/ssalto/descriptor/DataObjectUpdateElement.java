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
 * Cette classe permet de representer un element DATA_OBJECT_UPDATE qui peut etre cree par les differents process de
 * SIPAD-SSALTO un dataObjectUpdate se fait sur la liste de identifiants des objets de stockage uniquement
 * 
 * @author Christophe Mertz
 */

public class DataObjectUpdateElement extends DataObjectElement {

    /**
     * renvoie UPDATE_ELEMENT_TYPE
     */
    @Override
    public ElementType getElementType() {
        return ElementType.UPDATE_ELEMENT_TYPE;
    }

    /**
     * renvoie un ordre pour pouvoir etre trie dans les fichiers descripteurs
     */
    @Override
    protected int getOrder() {
        return 4;
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
        DataObjectUpdateElement other = (DataObjectUpdateElement) obj;
        if (getDataObjectIdentifier() == null) {
            if (other.getDataObjectIdentifier() != null) {
                return false;
            }
        } else if (!getDataObjectIdentifier().equals(other.getDataObjectIdentifier())) {
            return false;
        }
        return true;
    }
}
