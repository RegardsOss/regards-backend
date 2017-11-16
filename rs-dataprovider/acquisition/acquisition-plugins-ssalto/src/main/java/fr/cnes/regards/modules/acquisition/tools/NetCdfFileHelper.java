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
package fr.cnes.regards.modules.acquisition.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import ucar.ma2.Array;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class NetCdfFileHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(NetCdfFileHelper.class);

    private NetcdfFile netCfdFile;

    public NetCdfFileHelper(File pNetCdfFile) {
        try {
            netCfdFile = NetcdfFile.open(pNetCdfFile.getAbsolutePath());
        } catch (IOException ioe) {
            LOGGER.error("trying to open " + pNetCdfFile.getAbsolutePath(), ioe);
        }
    }

    public void release() {
        try {
            netCfdFile.close();
        } catch (IOException ioe) {
            LOGGER.error("trying to close " + netCfdFile.getLocation(), ioe);
        }
    }

    public String getGlobalAttributeStringValue(String pAttributeName, String pFormatRead) {
        String attValue = netCfdFile.findGlobalAttribute(pAttributeName).getStringValue();
        if (pFormatRead != null) {
            attValue = attValue.substring(0, pFormatRead.length());
        }
        return attValue;
    }

    /**
     * renvoie la liste des valeurs (chaine de caracteres) de l'attribut pAttributeName de toutes les variables du
     * fichier
     * 
     * @param pAttributeName
     *            le nom de l'attribut a recuperer dans les variables.
     * @param pExceptionList
     *            une liste d'exception sur les noms des variables.
     * @return
     */
    public List<String> getAllVariableAttributeValue(String pAttributeName, List<String> pExceptionList) {
        if (pExceptionList == null) {
            pExceptionList = new ArrayList<>();
        }
        List<String> resultList = new ArrayList<>();
        for (Variable element : netCfdFile.getVariables()) {
            if (!pExceptionList.contains(element.getShortName())) {
                Attribute att = element.findAttribute(pAttributeName);
                resultList.add(att.getStringValue());
            } else {
                LOGGER.debug("variable " + element.getShortName() + " skipped because present in exception list");
            }
        }
        return resultList;
    }

    /**
     * renvoie la liste des valeurs pour la variable donnee
     * 
     * @param pVariableName
     *            le nom de la variable
     * @param pValueType
     *            le type de valeur a recuperer.
     * @return
     */
    public List<Object> getVariableValues(String pVariableName, AttributeTypeEnum pValueType) {
        Variable longitude = netCfdFile.findVariable(pVariableName);
        List<Object> valueList = new ArrayList<>();
        double scale = 1;
        if (longitude.findAttribute("scale_factor") != null) {
            scale = longitude.findAttribute("scale_factor").getNumericValue().doubleValue();
        }
        try {
            Array longArray = longitude.read();
            LOGGER.debug(longArray.getElementType().getName());
            IndexIterator iter = longArray.getIndexIterator();
            for (; iter.hasNext();) {
                if (pValueType.equals(AttributeTypeEnum.TYPE_INTEGER)
                        || pValueType.equals(AttributeTypeEnum.TYPE_REAL)) {
                    int value = iter.getIntNext();
                    valueList.add(Double.valueOf(value * scale));
                } else {
                    Object value = iter.getObjectNext();
                    valueList.add(value);
                }
            }
        } catch (IOException e) {
            LOGGER.error("", e);
        }
        return valueList;
    }

}
