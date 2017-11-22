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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginConfigurationProperties;

/**
 * classe mere des finder qui trouvent les valeurs des attributs dans le nom des fichiers
 * 
 * @author Christophe Mertz
 *
 */
public class FileNameFinder extends AttributeFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileNameFinder.class);

    /**
     * filePattern a utiliser pour lire le nom du fichier et en extraire les valeurs
     */
    protected String filePattern;

    /**
     * liste des groupes de captures utilises pour recuperer les valeur dans le nom du fichier
     */
    protected List<Integer> groupNumberList;

    /**
     * filePattern a utiliser pour lire le nom du fichier dans l'archive ZIP et en extraire les valeurs
     */
    protected String fileInZipNamepattern;

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | filePattern : " + filePattern);
        buff.append(" | groupNumberList : ");
        for (Iterator<Integer> gIter = groupNumberList.iterator(); gIter.hasNext();) {
            Integer group = gIter.next();
            buff.append(group);
            if (gIter.hasNext()) {
                buff.append(",");
            }
        }
        return buff.toString();
    }

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        LOGGER.debug("begin");
        // List<Object> le type des objets depend du type AttributeTypeEnum
        List<Object> valueList = new ArrayList<>();
        // extrait les fichiers a partir des ssalto File
        List<File> fileToProceedList = buildFileList(fileMap);

        Pattern pattern = Pattern.compile(filePattern);

        Object parsedValue = null;
        for (File fileToProceed : fileToProceedList) {
            Matcher matcher = pattern.matcher(fileToProceed.getName());
            LOGGER.debug("testing file " + fileToProceed.getName());
            if (matcher.matches()) {
                StringBuffer value = new StringBuffer();

                // la valeur finale peut etre compose de plusieurs groupes
                for (Object element : groupNumberList) {
                    Integer groupNumber = (Integer) element;
                    value.append(matcher.group(groupNumber.intValue()));
                }
                parsedValue = valueOf(value.toString());

                if (parsedValue != null) {
                    LOGGER.debug("add value " + parsedValue.toString() + " for file " + fileToProceed.getName());
                }
            }
        }
        if (parsedValue != null) {
            valueList.add(parsedValue);
        }
        // warn if no file are found
        if (valueList.isEmpty()) {
            String msg = "No filename matching the pattern " + filePattern;
            LOGGER.warn(msg);
        }
        LOGGER.debug("end");
        return valueList;
    }

    public void addGroupNumber(String newGroupNumber) {
        if (groupNumberList == null) {
            groupNumberList = new ArrayList<>();
        }
        groupNumberList.add(Integer.valueOf(newGroupNumber));
    }

    @Override
    public void setAttributProperties(PluginConfigurationProperties confProperties) {
        super.setAttributProperties(confProperties);
        filePattern = confProperties.getFileNamePattern();
    }

    public void setFileInZipNamePattern(String fileInZipNamePattern) {
        fileInZipNamepattern = fileInZipNamePattern;
    }
}
