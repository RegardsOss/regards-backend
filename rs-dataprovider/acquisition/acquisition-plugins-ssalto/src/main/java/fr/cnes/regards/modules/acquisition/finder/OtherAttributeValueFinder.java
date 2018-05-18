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
package fr.cnes.regards.modules.acquisition.finder;

import java.io.File;
import java.util.List;
import java.util.Map;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;

/**
 * Ce finder cherche la valeur d'un attribut uniquement a partir de la valeur d'un autre attribut. Un calcul est
 * possible en instanciant et en appelant la class calculationClass
 * 
 * @author Christophe Mertz
 *
 */
public class OtherAttributeValueFinder extends AbstractAttributeFinder {

    /**
     * Nom de l'attribut dont on doit recuperer la valeur dans la attributeValueMap
     */
    private String otherAttributeName;

    @Override
    public List<Object> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        @SuppressWarnings("unchecked")
        List<Object> otherValueList = (List<Object>) attributeValueMap.get(otherAttributeName);

        if (otherValueList == null) {
            String msg = "unable to find the value for attribute " + otherAttributeName + " Check finder ordering.";
            throw new PluginAcquisitionException(msg);
        } else {
            // La liste otheValueList est une liste de String car c'est la valeur XML
        }
        return otherValueList;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | otherAttributeName").append(otherAttributeName);
        return buff.toString();
    }

    public void setOtherAttributeName(String newOtherAttributeName) {
        otherAttributeName = newOtherAttributeName;
    }

    protected String getOtherAttributName() {
        return otherAttributeName;
    }
}
