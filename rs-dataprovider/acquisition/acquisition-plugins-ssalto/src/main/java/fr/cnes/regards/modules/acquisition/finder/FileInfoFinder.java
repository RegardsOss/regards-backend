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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.tools.FileInformationTypeEnum;

/**
 * Ce finder doit aller chercher la valeur de l'attribut dans les informations systemes du fichier.
 * 
 * @author Christophe Mertz
 *
 */
public class FileInfoFinder extends AbstractAttributeFinder {

    /**
     * le type d'information a aller chercher
     */
    private FileInformationTypeEnum fileInformation;

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        List<Object> valueList = new ArrayList<>();
        for (File file : fileMap.keySet()) {
            File originalFile = (File) fileMap.get(file);
            if (fileInformation.equals(FileInformationTypeEnum.LAST_MODIFICATION_DATE)) {
                if (!valueList.isEmpty()) {
                    // check if date inside the list is before the new Date
                    OffsetDateTime oldOffSetDateTime = (OffsetDateTime) valueList.get(0);

                    Date newDate = new Date(originalFile.lastModified());
                    OffsetDateTime newOffsetDateTime = OffsetDateTime.ofInstant(newDate.toInstant(), ZoneId.of("UTC"));

                    if (oldOffSetDateTime.isBefore(newOffsetDateTime)) {
                        valueList.clear();
                        valueList.add(0, newOffsetDateTime);
                    }
                } else {
                    Date date = new Date(originalFile.lastModified());
                    OffsetDateTime offDateTime = OffsetDateTime.ofInstant(date.toInstant(), ZoneId.of("UTC"));
                    valueList.add(0, offDateTime);
                }
            } else if (fileInformation.equals(FileInformationTypeEnum.FILE_SIZE)) {
                if (!valueList.isEmpty()) {
                    Long oldLong = (Long) valueList.get(0);
                    valueList.clear();
                    valueList.add(Long.valueOf(oldLong.longValue() + originalFile.length()));
                } else {
                    valueList.add(Long.valueOf(originalFile.length()));
                }
            }
        }
        return valueList;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | fileInfo : ").append(fileInformation);
        return buff.toString();
    }

    public void setFileInformation(String newFileInformation) {
        fileInformation = FileInformationTypeEnum.parse(newFileInformation);
    }
}
