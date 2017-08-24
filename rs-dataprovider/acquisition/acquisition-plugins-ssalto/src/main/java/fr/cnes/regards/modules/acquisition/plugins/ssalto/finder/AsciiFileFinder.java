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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.RinexFileHelper;

/** */
public class AsciiFileFinder extends DataFileFinder {

    /**
     * filePattern de la ligne a parser
     */
    public String linePattern;

    /**
     * numero de la ligne a parser
     */
    public int lineNumber;

    /**
     * liste de groupe de capture a recupere dans la ligne pour construire la valeur de l'attribut
     */
    public List<Integer> groupNumberList;

    /**
     * Attribut renseigne lorsqu'on a besoin de traiter un seul fichier pour un produit contenant plusieurs fichiers.
     */
    public String filterPattern = null;

    @Override
    public List<?> getValueList(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        Pattern pattern = Pattern.compile(linePattern);
        List<Object> valueList = new ArrayList<>();
        for (File file : buildFileList(pFileMap)) {
            RinexFileHelper helper = new RinexFileHelper(file);
            String value = "";
            for (Object element : groupNumberList) {
                Integer groupNumber = (Integer) element;
                value = value + helper.getValue(lineNumber, pattern, groupNumber.intValue());
            }
            valueList.add(valueOf(value));
        }
        return valueList;
    }

    @Override
    protected List<File> buildFileList(Map<File, ?> pFileMap) throws PluginAcquisitionException {
        List<File> fileList = super.buildFileList(pFileMap);
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
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
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

    public void setLinePattern(String pPatternLine) {
        linePattern = pPatternLine;
    }

    public void setLineNumber(String pLineNumber) {
        lineNumber = Integer.parseInt(pLineNumber);
    }

    public void addGroupNumber(String pGroupNumber) {
        if (groupNumberList == null) {
            groupNumberList = new ArrayList<>();
        }
        groupNumberList.add(new Integer(pGroupNumber));
    }

    public void setFilterPattern(String pFilterPattern) {
        filterPattern = pFilterPattern;
    }

}
