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
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.CalculusTypeEnum;

/**
 * finder qui recuperer la valeur dans une liste de fichiers, puis  effectue eventuellement un calcul avec ces valeurs pour
 * renvoyer la valeur de l'attribut
 * 
 * @author Christophe Mertz
 *
 */
public class MultipleFileNameFinder extends FileNameFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFileNameFinder.class);

    /**
     * type de calcul a effectuer a partir des valeurs recuperees dans chaque fichier
     */
    private CalculusTypeEnum calculus;

    @Override
    public List<?> getValueList(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        LOGGER.debug("--> begin");
        // List<Object> le type des objets depend du type AttributeTypeEnum
        List<Object> valueList = new ArrayList<>();
        // extrait les fichiers a partir des ssalto File
        List<File> fileToProceedList = buildFileList(fileMap);

        Pattern pattern = Pattern.compile(filePattern);

        Object parsedValue = null;
        for (File fileToProceed : fileToProceedList) {
            Matcher matcher = pattern.matcher(fileToProceed.getName());
            if (matcher.matches()) {

                String value = "";
                // la valeur finale peut etre compose de plusieurs groupes
                for (Integer groupNumber : groupNumberList) {
                    value = value + matcher.group(groupNumber.intValue());
                }

                parsedValue = valueOf(value);
                LOGGER.debug("add value " + parsedValue.toString() + " for file " + fileToProceed.getName());
                if (parsedValue != null) {
                    valueList.add(parsedValue);
                }
            }
        }

        // on applique le calcul sur les resultats
        List<Object> processedValuesList = processCalculOnValues(valueList);
        // warn if no file are found
        if (valueList.isEmpty()) {
            String msg = "No filename matching pattern " + filePattern;
            LOGGER.warn(msg);
        }
        LOGGER.debug("<-- end");

        return processedValuesList;
    }

    @Override
    protected List<File> buildFileList(Map<File, ?> acquisitionFileMap) throws PluginAcquisitionException {
        if (fileInZipNamepattern != null) {
            filePattern = fileInZipNamepattern;
        }
        return super.buildFileList(acquisitionFileMap);

    }

    /**
     * Execute le mode de calcul sur la liste
     * 
     * @param newValuesList
     *            List <Object>
     * @return List <Object>
     * @throws PluginAcquisitionException
     */
    private List<Object> processCalculOnValues(List<Object> newValuesList) throws PluginAcquisitionException {
        // List <Object> de taille 1 correspondant a la moyenne au min et au max de pValuesList
        List<Object> calculatedList = null;
        if (newValuesList.isEmpty() || (calculus == null)) {
            calculatedList = newValuesList;
        } else {
            // s'il n'y a pas de fonction de calcul on renvoie l'enumere
            if (calculus.equals(CalculusTypeEnum.AVG)) {
                calculatedList = getAverage(newValuesList);
            } else if (calculus.equals(CalculusTypeEnum.MIN) || calculus.equals(CalculusTypeEnum.MAX)) {
                calculatedList = getSortedList(newValuesList);
            }
        }
        return calculatedList;
    }

    /**
     * Cree une liste de taille 1 avec l'element MIN ou MAX
     * 
     * @param newValuesList
     *            {@link List} of {@link Object}
     * @return List <Object> contenant l'element min ou max de pValuesList
     * @throws PluginAcquisitionException
     */
    private List<Object> getSortedList(List<Object> newValuesList) throws PluginAcquisitionException {
        List<Object> sortedList;
        // cree un sorted set a partir de la liste
        SortedSet<Object> sortedValueSet = new TreeSet<>();
        sortedValueSet.addAll(newValuesList);
        sortedList = new ArrayList<>(1);
        if (calculus.equals(CalculusTypeEnum.MIN)) {
            sortedList.add(sortedValueSet.first());
        } else if (calculus.equals(CalculusTypeEnum.MAX)) {
            sortedList.add(sortedValueSet.last());
        } else {
            // ne doit pas arriver
            String msg = "Calculus type must be MIN or MAX";
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg);
        }
        return sortedList;
    }

    /**
     * 
     * @param valuesList
     * @return
     */
    // TODO CMZ à confirmer
    private List<Object> getAverage(List<Object> valuesList) {
        return valuesList;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append(super.toString());
        buff.append(" | calculus").append(calculus.toString());
        return buff.toString();
    }

    public void setCalculus(String newCalculus) {
        calculus = CalculusTypeEnum.parse(newCalculus);
    }

}
