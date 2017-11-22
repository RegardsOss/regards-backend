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

import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.tools.RinexFileHelper;

/**
 * 
 * @author Christophe Mertz
 *
 */
public class AsciiFileFinder extends DataFileFinder {

    /**
     * filePattern de la ligne a parser
     */
    private String linePattern;

    /**
     * numero de la ligne a parser
     */
    private int lineNumber;

    /**
     * liste de groupe de capture a recupere dans la ligne pour construire la valeur de l'attribut
     */
    private final List<Integer> groupNumberList = new ArrayList<>();

    /**
     * Attribut renseigne lorsqu'on a besoin de traiter un seul fichier pour un produit contenant plusieurs fichiers.
     */
    private String filterPattern = null;

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        Pattern pattern = Pattern.compile(linePattern);
        List<Object> valueList = new ArrayList<>();
        for (File file : buildFileList(fileMap)) {
            RinexFileHelper helper = new RinexFileHelper(file);
            StringBuffer value = new StringBuffer();
            for (Object element : groupNumberList) {
                Integer groupNumber = (Integer) element;
                value.append(helper.getValue(lineNumber, pattern, groupNumber.intValue()));
            }
            valueList.add(valueOf(value.toString()));
        }
        return valueList;
    }

    @Override
    protected List<File> buildFileList(Map<File, ?> fileMap) throws PluginAcquisitionException {
        List<File> fileList = super.buildFileList(fileMap);
        if (filterPattern != null) {
            // Backup file list
            List<File> oldList = fileList;
            // Init result file list
            fileList = new ArrayList<>();
            // Select only file(s) that match(es) the filter filePattern
            Pattern pattern = Pattern.compile(filterPattern);
            for (File tmp : oldList) {
                Matcher match = pattern.matcher(tmp.getName());
                if (match.matches()) {
                    fileList.add(tmp);
                }
            }
        }
        return fileList;
    }

    @Override
    public String toString() {
        StringBuilder buff = new StringBuilder(super.toString());
        buff.append(" | patternLine : ").append(linePattern);
        buff.append(" | lineNumber : ").append(lineNumber);
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

    public void setLinePattern(String newLinePattern) {
        linePattern = newLinePattern;
    }

    public void setLineNumber(String newLineNumber) {
        lineNumber = Integer.parseInt(newLineNumber);
    }

    public void addGroupNumber(String groupNumber) {
        groupNumberList.add(Integer.valueOf(groupNumber));
    }

    public void setFilterPattern(String newFilterPattern) {
        filterPattern = newFilterPattern;
    }

}
