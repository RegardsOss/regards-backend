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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.tools.CalculusTypeEnum;
import fr.cnes.regards.modules.acquisition.tools.NetCdfFileHelper;

/**
 * Ce finder doit recuperer l'attribut d'une variable dans un fichier netCDF. <br>
 * Il peut y avoir plusieurs variables trouvees dans le fichier. <br>
 * Un calcul est alors necessaire pour recuperer la valeur a renvoyer.
 * 
 * @author Christophe Mertz
 *
 */
public class CDFVariableAttributeValueFinder extends AbstractCdfFileFinder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(CDFVariableAttributeValueFinder.class);

    /**
     * nom de la variable a chercher dans le fichier netCDF
     */
    private String variableName;

    /**
     * calcul a effectuer sur la liste des valeurs de l'attribut sur toutes les variables trouvees dans le fichier
     */
    private CalculusTypeEnum calculus;

    @Override
    public List<Object> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        List<Object> translatedValueList = new ArrayList<>();
        for (File file : buildFileList(fileMap)) {
            NetCdfFileHelper helper = new NetCdfFileHelper(file);
            List<Object> values = helper.getVariableValues(variableName, getValueType());
            helper.release();

            if (calculus.equals(CalculusTypeEnum.FIRST)) {
                Object value = values.get(0);
                if (calculationClass != null) {
                    value = calculationClass.calculateValue(value, getValueType(), confProperties);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[{}] first value found {}", variableName, value.toString());
                }
                translatedValueList.add(value);
                break;
            } else if (calculus.equals(CalculusTypeEnum.LAST)) {
                Object value = values.get(values.size() - 1);
                if (calculationClass != null) {
                    value = calculationClass.calculateValue(value, getValueType(), confProperties);
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("[{}] last value found {}", variableName, value.toString());
                }
                translatedValueList.add(value);
            } else {
                // apply the calculus class transformation
                for (Object value : values) {
                    // launch calculation if needed
                    if (calculationClass != null) {
                        value = calculationClass.calculateValue(value, getValueType(), confProperties);
                    }
                    if (translatedValueList.isEmpty()) {
                        translatedValueList.add(value);
                    } else {
                        @SuppressWarnings("unchecked")
                        int compare = ((Comparable<Object>) value).compareTo(translatedValueList.get(0));
                        if (calculus.equals(CalculusTypeEnum.MAX) && (compare > 0)) {
                            translatedValueList.clear();
                            translatedValueList.add(value);
                        }
                        if (calculus.equals(CalculusTypeEnum.MIN) && (compare < 0)) {
                            translatedValueList.clear();
                            translatedValueList.add(value);
                        }
                    }
                }
            }
        }
        return translatedValueList;
    }

    /**
     * Ne fait pas la translation car elle a deja ete faite.
     */
    @Override
    protected List<Object> translateValueList(List<? extends Object> newValueList) {
        @SuppressWarnings("unchecked")
        List<Object> valueList = (List<Object>) newValueList;
        return valueList;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | variableName").append(variableName);
        buff.append(" | calculus").append(calculus);
        return buff.toString();
    }

    public void setCalculus(String newCalculus) {
        calculus = CalculusTypeEnum.parse(newCalculus);
    }

    public void setVariableName(String newVariableName) {
        variableName = newVariableName;
    }
}
