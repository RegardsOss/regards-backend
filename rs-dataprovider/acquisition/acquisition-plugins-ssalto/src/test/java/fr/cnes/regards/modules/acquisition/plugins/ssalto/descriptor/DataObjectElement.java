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

import java.util.Set;
import java.util.TreeSet;

/**
 * Cette classe est la classe mere des elements concernant les dataObject Elle contient des methodes pour generer les
 * blocs xml communs au update et description
 * 
 * @author Christophe Mertz
 */

public abstract class DataObjectElement extends EntityDescriptorElement {

    private String dataObjectIdentifier;

    protected Set<String> dataStorageObjectIdentifiers;

    /**
     * renvoie le dataObjectIdentifier
     * 
     * @see ssalto.domain.data.descriptor.EntityDescriptorElement#getEntityId()
     */
    @Override
    public String getEntityId() {
        return dataObjectIdentifier;
    }

    /**
     * ajoute un dataStorageObjectIdentifier a la liste de l'element
     * 
     * @param pDataStorageObjectIdentifier
     */
    public void addDataStorageObjectIdentifier(String pDataStorageObjectIdentifier) {
        if (dataStorageObjectIdentifiers == null) {
            dataStorageObjectIdentifiers = new TreeSet<>();
        }
        dataStorageObjectIdentifiers.add(pDataStorageObjectIdentifier);
    }

    public String getDataObjectIdentifier() {
        return dataObjectIdentifier;
    }

    public void setDataObjectIdentifier(String pDataObjectIdentifier) {
        dataObjectIdentifier = pDataObjectIdentifier;
    }

    public Set<String> getDataStorageObjectIdentifiers() {
        return dataStorageObjectIdentifiers;
    }

    public void setDataStorageObjectIdentifiers(Set<String> pDataStorageObjectIdentifiers) {
        dataStorageObjectIdentifiers = pDataStorageObjectIdentifiers;
    }

}
