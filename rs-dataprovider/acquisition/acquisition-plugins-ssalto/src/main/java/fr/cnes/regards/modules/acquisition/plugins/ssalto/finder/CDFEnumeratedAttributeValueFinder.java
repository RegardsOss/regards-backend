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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.finder;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.NetCdfFileHelper;

/**
 * Ce finder a pour but de lister les valeurs possible prises par l'attribut de toutes les variables d'un fichier au
 * format NetCDF
 * 
 * @author Christophe Mertz
 */
public class CDFEnumeratedAttributeValueFinder extends CdfFileFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDFEnumeratedAttributeValueFinder.class);

    private List<String> exceptionList_;

    @Override
    public Attribute buildAttribute(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        LOGGER.debug("START building attribute " + getName());
        CompositeAttribute composedAttribute = new CompositeAttribute();
        // important, set to null, to not create a root element and add directly
        // the attributes
        // into the dataObjectDescription XML bloc.
        // composedAttribute.setName(null);
        try {
            List<Object> valueList = getValueList(fileMap, attributeValueMap);
            // add attribut to calculated attribut map
            attributeValueMap.put(name, valueList);
            for (Object value : valueList) {
                if (calculationClass != null) {
                    value = calculationClass.calculateValue(value, getValueType(), confProperties);
                }
                // translate the value
                String translatedValue = changeFormat(value);
                List<String> translatedValueList = new ArrayList<>();
                translatedValueList.add(translatedValue);
                LOGGER.debug("adding value " + translatedValue);
                Attribute attribute = AttributeFactory.createAttribute(getValueType(), getName(), translatedValueList);
                composedAttribute.addAttribute(attribute);
            }
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + getName();
            throw new PluginAcquisitionException(msg, e);
        }
        LOGGER.debug("START building attribute " + getName());
        return composedAttribute;
    }

    /**
     * va chercher pour chaque fichier la valeur
     */
    @Override
    public List<Object> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        List<Object> valueList = new ArrayList<>();
        for (File file : buildFileList(fileMap)) {
            NetCdfFileHelper helper = new NetCdfFileHelper(file);
            for (String value : helper.getAllVariableAttributeValue(attributeName, exceptionList_)) {
                valueList.add(changeFormat(value));
            }
            helper.release();
        }
        return valueList;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append(" | exceptList : ");
        for (Iterator<String> gIter = exceptionList_.iterator(); gIter.hasNext();) {
            String except = gIter.next();
            buff.append(except);
            if (gIter.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

    public void addException(String except) {
        if (exceptionList_ == null) {
            exceptionList_ = new ArrayList<>();
        }
        exceptionList_.add(except);
    }
}
