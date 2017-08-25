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
package fr.cnes.regards.modules.acquisition.plugins.ssalto;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.MultipleFileNameFinder;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.RinexFileHelper;

/**
 * plugin specifiques au donnees saral doris les noms des fichiers ont deux formes bien distinctes et ne peuvent pas
 * etre resolues juste par le fichier de configuration. Les attributs traites specifiquement sont les TIME_PERIOD et
 * FILE_CREATION_DATE.
 * 
 * @author Christophe Mertz
 */

public class SaralDoris10ProductMetadataPlugin extends SaralProductMetadataPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultipleFileNameFinder.class);

    private static final String TIME_PERIOD = "TIME_PERIOD";

    private static final String START_DATE = "START_DATE";

    private static final String STOP_DATE = "STOP_DATE";

    private static final String CREATION_DATE = "FILE_CREATION_DATE";

    private static final Pattern CREATION_DATE_PATTERN = Pattern.compile(".* ([0-9]{8} [0-9]{6}) UTC.*");

    protected Pattern patternd;

    protected Pattern patternp;

    protected Pattern patternh;

    protected Pattern patternl;

    public SaralDoris10ProductMetadataPlugin() {
        super();
    }

    /**
     * ajoute l'initialisation du filePattern des fichiers en fonction du filePattern generique. Methode surchargee
     * 
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.SaralProductMetadataPlugin#init(java.lang.String)
     */
    @Override
    public void init(String pDataSetName) throws ModuleException {
        super.init(pDataSetName);
        String fileNamePattern = getProperties().getFileNamePattern();
        String prefix = fileNamePattern.substring(0, fileNamePattern.indexOf("("));
        patternd = Pattern.compile(prefix + "[A-Z]([0-9]{8}_[0-9]{6})$");
        patternp = Pattern.compile(prefix + "[A-Z]([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})$");
        patternh = Pattern.compile(prefix + "([a-z]{1})([DS]{1})([0-9]{8}_[0-9]{6})$");
        patternl = Pattern
                .compile(prefix + "([a-z]{1})([DS]{1})([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})$");
    }

    /**
     * cree les attributs time_period et file_creation_date Methode surchargee
     * 
     * 
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.SaralProductMetadataPlugin#doCreateIndependantSpecificAttributes(java.util.List,
     *      java.util.Map)
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        registerTimePeriodAttributes(pFileMap, pAttributeMap);
        registerFileCreationDateAttribute(pFileMap, pAttributeMap);
    }

    private void registerTimePeriodAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + TIME_PERIOD);
        CompositeAttribute timePeriodAttribute = new CompositeAttribute();
        timePeriodAttribute.setName(TIME_PERIOD);
        try {
            Attribute startDateAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_DATE_TIME,
                                                                            START_DATE,
                                                                            getStartDateValue(pFileMap.keySet()));
            timePeriodAttribute.addAttribute(startDateAttribute);
            attributeValueMap_.put(START_DATE, startDateAttribute.getValueList());

            Attribute stopDateAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_DATE_TIME, STOP_DATE,
                                                                           getStopDateValue(pFileMap.keySet()));
            timePeriodAttribute.addAttribute(stopDateAttribute);
            attributeValueMap_.put(STOP_DATE, stopDateAttribute.getValueList());
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + TIME_PERIOD;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg, e);
        }
        registerAttribute(TIME_PERIOD, pAttributeMap, timePeriodAttribute);
        LOGGER.info("END building attribute " + TIME_PERIOD);
    }

    protected List<Date> getStartDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        for (File file : pSsaltoFileList) {
            String fileName = file.getName();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            Matcher matcherH = patternh.matcher(fileName);
            Matcher matcherL = patternl.matcher(fileName);
            try {
                if (matcherD.matches()) {
                    String dateStr = matcherD.group(1);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else if (matcherP.matches()) {
                    String dateStr = matcherP.group(2);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else if (matcherH.matches()) {
                    String dateStr = matcherH.group(3);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else if (matcherL.matches()) {
                    String dateStr = matcherL.group(4);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else {
                    String msg = "filename does not match";
                    LOGGER.error(msg);
                    throw new PluginAcquisitionException(msg);
                }

            } catch (ParseException e) {
                throw new PluginAcquisitionException(e);
            }
        }
        return valueList;
    }

    protected List<Date> getStopDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        int i = 0;
        for (File file : pSsaltoFileList) {
            String fileName = file.getName();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            Matcher matcherH = patternh.matcher(fileName);
            Matcher matcherL = patternl.matcher(fileName);
            try {
                if (matcherD.matches()) {
                    @SuppressWarnings("unchecked")
                    List<Date> startDateValueList = (List<Date>) attributeValueMap_.get(START_DATE);
                    Date date = startDateValueList.get(i);
                    long newTime = date.getTime() + 86400000;
                    valueList.add(new Date(newTime));
                } else if (matcherP.matches()) {
                    String dateStr = matcherP.group(3);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else if (matcherH.matches()) {
                    @SuppressWarnings("unchecked")
                    List<Date> startDateValueList = (List<Date>) attributeValueMap_.get(START_DATE);
                    Date date = startDateValueList.get(i);
                    long newTime = date.getTime() + 86400000;
                    valueList.add(new Date(newTime));
                } else if (matcherL.matches()) {
                    String dateStr = matcherL.group(5);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else {
                    String msg = "filename does not match";
                    LOGGER.error(msg);
                    throw new PluginAcquisitionException(msg);
                }

            } catch (ParseException e) {
                throw new PluginAcquisitionException(e);
            }
            i++;
        }
        return valueList;
    }

    protected List<Date> getCreationDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        for (File file : pSsaltoFileList) {
            String fileName = file.getName();
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            SimpleDateFormat fileFormat = new SimpleDateFormat("yyyyMMdd HHmmss");
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            Matcher matcherH = patternh.matcher(fileName);
            Matcher matcherL = patternl.matcher(fileName);
            try {
                if (matcherD.matches()) {
                    // go to search into file using RINExFileHelper
                    RinexFileHelper helper = new RinexFileHelper(file);
                    Pattern cdPattern = CREATION_DATE_PATTERN;
                    String dateStr = helper.getValue(2, cdPattern, 1);
                    Date date = fileFormat.parse(dateStr);
                    valueList.add(date);
                } else if (matcherP.matches()) {
                    String dateStr = matcherP.group(1);
                    Date date = fileNameFormat.parse(dateStr);
                    valueList.add(date);
                } else if (matcherH.matches()) {
                    // go to search into file using RINExFileHelper
                    RinexFileHelper helper = new RinexFileHelper(file);
                    Pattern cdPattern = CREATION_DATE_PATTERN;
                    String dateStr = helper.getValue(2, cdPattern, 1);
                    Date date = fileFormat.parse(dateStr);
                    valueList.add(date);
                } else if (matcherL.matches()) {
                    String dateStr = matcherL.group(3);
                    Date date = fileNameFormat.parse(dateStr);
                    valueList.add(date);
                } else {
                    String msg = "filename does not match";
                    LOGGER.error(msg);
                    throw new PluginAcquisitionException(msg);
                }

            } catch (ParseException e) {
                throw new PluginAcquisitionException(e);
            }
        }
        return valueList;
    }

    private void registerFileCreationDateAttribute(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + CREATION_DATE);

        try {
            Attribute fileCreationDateAttribute = AttributeFactory
                    .createAttribute(AttributeTypeEnum.TYPE_DATE_TIME, CREATION_DATE,
                                     getCreationDateValue(pFileMap.keySet()));
            registerAttribute(CREATION_DATE, pAttributeMap, fileCreationDateAttribute);
            attributeValueMap_.put(CREATION_DATE, fileCreationDateAttribute.getValueList());
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + CREATION_DATE;
            throw new PluginAcquisitionException(msg, e);
        }

        LOGGER.info("END building attribute " + CREATION_DATE);
    }
}
