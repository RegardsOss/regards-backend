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

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(NetCdfFileHelper.class);

    /**
     * Read-only scientific datasets that are accessible through the netCDF API
     */
    private NetcdfFile netCfdFile;

    /**
     * Constructor
     * @param pNetCdfFile the Netcdf file to read
     */
    public NetCdfFileHelper(File pNetCdfFile) {
        try {
            netCfdFile = NetcdfFile.open(pNetCdfFile.getAbsolutePath());
        } catch (IOException ioe) {
            LOGGER.error("trying to open " + pNetCdfFile.getAbsolutePath(), ioe);
        }
    }

    /**
     * Close the current Netcdf file
     */
    public void release() {
        try {
            netCfdFile.close();
        } catch (IOException ioe) {
            LOGGER.error("trying to close " + netCfdFile.getLocation(), ioe);
        }
    }

    /**
     * Get a substring of an attribute value. The attribute is loop up by name.  
     * @param attributeName the attribute name to find
     * @param formatRead the format used to substring the attribute value. Only the format length is used to substrings.
     * @return the substring of an attribute value
     */
    public String getGlobalAttributeStringValue(String attributeName, String formatRead) {
        String attValue = netCfdFile.findGlobalAttribute(attributeName).getStringValue();
        if (formatRead != null) {
            attValue = attValue.substring(0, formatRead.length());
        }
        return attValue;
    }

    /**
     * Renvoie la liste des valeurs (chaine de caracteres) de l'attribut pAttributeName de toutes les variables du fichier
     * 
     * @param attributeName
     *            le nom de l'attribut a recuperer dans les variables.
     * @param exceptionList
     *            une liste d'exception sur les noms des variables.
     * @return
     */
    public List<String> getAllVariableAttributeValue(String attributeName, List<String> exceptionList) {
        if (exceptionList == null) {
            exceptionList = new ArrayList<>();
        }
        List<String> resultList = new ArrayList<>();
        for (Variable element : netCfdFile.getVariables()) {
            if (!exceptionList.contains(element.getShortName())) {
                Attribute att = element.findAttribute(attributeName);
                resultList.add(att.getStringValue());
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("variable " + element.getShortName() + " skipped because present in exception list");
                }
            }
        }
        return resultList;
    }

    /**
     * Renvoie la liste des valeurs pour la variable donnee
     * 
     * @param variable
     *            le nom de la variable
     * @param valueType
     *            le type de valeur a recuperer.
     * @return
     */
    public List<Object> getVariableValues(String variable, AttributeTypeEnum valueType) {
        Variable longitude = netCfdFile.findVariable(variable);
        List<Object> valueList = new ArrayList<>();
        double scale = 1;
        if (longitude.findAttribute("scale_factor") != null) {
            scale = longitude.findAttribute("scale_factor").getNumericValue().doubleValue();
        }
        try {
            Array longArray = longitude.read();
            IndexIterator iter = longArray.getIndexIterator();
            for (; iter.hasNext();) {
                if (valueType.equals(AttributeTypeEnum.TYPE_INTEGER) || valueType.equals(AttributeTypeEnum.TYPE_REAL)) {
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
