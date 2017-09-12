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

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;

/**
 * Ce finder permet de construire une classe CompositeAttribute avec les attributs trouves par les
 * finder qui lui ont ete associes
 * 
 * @author Christophe Mertz
 *
 */
public class CompositeAttributeFinder extends AttributeFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeAttributeFinder.class);

    /**
     * liste de finder associes a l'attribute compose
     */
    private List<AttributeFinder> finderList;

    /**
     * Nom de l'attribut compose, sera utilise dans le fichier descripteur pour construire la balise d' l'attribut
     * compose
     */
    private String name;

    @Override
    public Attribute buildAttribute(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {

        LOGGER.debug("START building composite attribute " + getName());

        CompositeAttribute attribute = new CompositeAttribute();
        attribute.setName(name);
        for (AttributeFinder finder : finderList) {
            Attribute attribut = finder.buildAttribute(fileMap, attributeValueMap);
            attribute.addAttribute(attribut);
        }

        LOGGER.debug("END building composite attribute " + getName());

        return attribute;
    }

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        // NOTHING TO DO
        return null;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(getClass().toString());
        buff.append(" | name : ").append(name).append("\n");
        for (Iterator<AttributeFinder> gIter = finderList.iterator(); gIter.hasNext();) {
            AttributeFinder finder = gIter.next();
            buff.append(finder.toString());
            if (gIter.hasNext()) {
                buff.append("\n");
            }
        }
        return buff.toString();
    }

    public void addFileFinder(AttributeFinder newFinder) {
        if (finderList == null) {
            finderList = new ArrayList<>();
        }
        finderList.add(newFinder);
    }

    @Override
    public void setAttributProperties(PluginConfigurationProperties confProperties) {
        for (AttributeFinder finder : finderList) {
            finder.setAttributProperties(confProperties);
        }
    }

    @Override
    public void setName(String newName) {
        name = newName;
    }

    @Override
    public String getName() {
        return name;
    }
}
