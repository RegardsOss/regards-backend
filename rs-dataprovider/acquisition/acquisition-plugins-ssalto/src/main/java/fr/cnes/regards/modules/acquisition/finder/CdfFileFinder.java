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
package fr.cnes.regards.modules.acquisition.finder;

/**
 * classe abstraite des finder de type dataFileFinder qui s'occupent des fichiers au format netCDF
 * 
 * @author Christophe Mertz
 *
 */
public abstract class CdfFileFinder extends DataFileFinder {

    /**
     * nom de l'attribut (CDF) a rechercher dans le fichier
     */
    protected String attributeName;

    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | attributeName").append(attributeName);
        return buff.toString();
    }

    public void setAttributeName(String newAttributeName) {
        attributeName = newAttributeName;
    }

    protected String getAttributeName() {
        return attributeName;
    }
}
