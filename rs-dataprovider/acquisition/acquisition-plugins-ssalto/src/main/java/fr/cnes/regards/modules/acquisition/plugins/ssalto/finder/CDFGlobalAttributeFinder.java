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
import java.util.List;
import java.util.Map;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.NetCdfFileHelper;

/**
 * Ce finder a pour role de recuperer la valeur d'un attribute global dans le fichier netCDF
 * 
 * @author Christophe Mertz
 *
 */
public class CDFGlobalAttributeFinder extends CdfFileFinder {

    public List<Object> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        List<Object> valueList = new ArrayList<>();
        for (File file : buildFileList(pFileMap)) {
            NetCdfFileHelper helper = new NetCdfFileHelper(file);
            String valueStr = helper.getGlobalAttributeStringValue(getAttributeName(), getFormatRead());
            helper.release();
            Object value = valueOf(valueStr);
            valueList.add(value);
        }
        return valueList;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
