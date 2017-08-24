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
import java.util.Date;
import java.util.List;
import java.util.Map;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.FileInformationTypeEnum;

/**
 * ce finder doit aller chercher la valeur de l'attribut dans les informations systemes du fichier
 * 
 * @author Christophe Mertz
 *
 */
public class FileInfoFinder extends AttributeFinder {

    /**
     * le type d'information a aller chercher
     */
    public FileInformationTypeEnum fileInformation;

    @Override
    public List<?> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        List<Object> valueList = new ArrayList<>();
        for (File file : pFileMap.keySet()) {
            File originalFile = (File) pFileMap.get(file);
            if (fileInformation.equals(FileInformationTypeEnum.LAST_MODIFICATION_DATE)) {
                if (!valueList.isEmpty()) {
                    // check if date inside the list is before the new Date
                    Date oldDate = (Date) valueList.get(0);
                    if (oldDate.after(new Date(originalFile.lastModified()))) {
                        // replace the value
                        valueList = new ArrayList<>();
                        valueList.add(0, new Date(originalFile.lastModified()));
                    }
                } else {
                    valueList.add(0, new Date(originalFile.lastModified()));
                }
            } else if (fileInformation.equals(FileInformationTypeEnum.FILE_SIZE)) {
                // reset the list
                valueList = new ArrayList<>();
                if (!valueList.isEmpty()) {
                    Long oldLong = (Long) valueList.get(0);
                    valueList = new ArrayList<>();
                    valueList.add(new Long(oldLong.longValue() + originalFile.length()));
                } else {
                    valueList.add(new Long(originalFile.length()));
                }
            }
        }
        return valueList;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append(" | fileInfo : ").append(fileInformation);
        return buff.toString();
    }

    public void setFileInformation(String pFileInformation) {
        fileInformation = FileInformationTypeEnum.parse(pFileInformation);
    }
}
