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
 * @author Christophe Mertz
 */

public abstract class EntityDescriptorElement implements Comparable<Object> {

    public enum ElementType {
        UPDATE_ELEMENT_TYPE,
        DESC_ELEMENT_TYPE
    };

    /**
     * renvoie l'identifiant de l'entite decrite par l'element
     * 
     * @return
     */
    public abstract String getEntityId();

    /**
     * renvoie le type de l'element
     * 
     * @return UPDATE_ELEMENT_TYPE ou DESC_ELEMENT_TYPE
     */
    public abstract ElementType getElementType();

    /**
     * renvoie un ordre pour pouvoir etre trie dans les fichiers descripteurs.
     */
    protected abstract int getOrder();

    /**
     * compare les ordres puis en cas d'egalite les identifiants
     */
    @Override
    public int compareTo(Object pArg0) {
        int result = getOrder() - ((EntityDescriptorElement) pArg0).getOrder();
        if (result == 0) {
            result = getEntityId().compareTo(((EntityDescriptorElement) pArg0).getEntityId());
        }
        return result;
    }
}