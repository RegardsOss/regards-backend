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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.RinexFileHelper;

/**
 * plugin specifiques au donnees jason1 Gpsp10Flot les noms des fichiers ont deux formes bien distinctes et ne peuvent
 * pas etre resolues juste par le fichier de configuration. L' attribut traite specifiquement est le TIME_PERIOD
 * 
 * @author Christophe Mertz
 * 
 */

public class Jason1Gpsp10FlotProductMetadataPlugin extends Jason1ProductMetadataPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Jason1Gpsp10FlotProductMetadataPlugin.class);

    private static final String TIME_PERIOD = "TIME_PERIOD";

    private static final String START_DATE = "START_DATE";

    private static final String STOP_DATE = "STOP_DATE";

    protected Pattern patternd;

    protected Pattern patternp;

    public Jason1Gpsp10FlotProductMetadataPlugin() {
        super();
    }

    /**
     * ajoute l'initialisation du filePattern des fichiers en fonction du filePattern generique
     */
    @Override
    public void init(String pDataSetName) throws ModuleException {
        super.init(pDataSetName);
        String fileNamePattern = getProperties().getFileNamePattern();
        String prefix = fileNamePattern.substring(0, fileNamePattern.indexOf("("));
        patternd = Pattern.compile(prefix + "[A-Z]([0-9]{8}_[0-9]{6})$");
        patternp = Pattern.compile(prefix + "[A-Z]([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})$");
    }

    /**
     * cree les attributs time_period et file_creation_date
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        registerTimePeriodAttributes(pFileMap, pAttributeMap);
    }

    /**
     * 
     * @param pFileMap
     * @param pAttributeMap
     * @throws PluginAcquisitionException
     */
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

    /**
     * 
     * @param pSsaltoFileList
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<Date> getStartDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : pSsaltoFileList) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMinValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue > valueRead) {
                longValue = valueRead;
            }
        }
        List<Date> valueList = new ArrayList<>();
        valueList.add(new Date(longValue));
        return valueList;
    }

    /**
     * 
     * @param pSsaltoFileList
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<Date> getStopDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : pSsaltoFileList) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMaxValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue < valueRead) {
                longValue = valueRead;
            }
        }
        List<Date> valueList = new ArrayList<>();
        valueList.add(new Date(longValue));
        return valueList;
    }

    /**
     * 
     * @param pSsaltoFileList
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<Date> getCreationDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        Date creationDate = null;
        try {
            for (File file : pSsaltoFileList) {
                RinexFileHelper helper = new RinexFileHelper(file);
                Pattern creationDatePattern = Pattern
                        .compile(".* ([\\d]{4}-[\\d]{2}-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2}) .*");
                String dateStr = helper.getValue(2, creationDatePattern, 1);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                Date dateRead = format.parse(dateStr);
                if (creationDate == null) {
                    creationDate = dateRead;
                } else if (creationDate.after(dateRead)) {
                    creationDate = dateRead;
                }
            }
            valueList.add(creationDate);
        } catch (ParseException e) {
            String msg = "unable to parse creation date";
            throw new PluginAcquisitionException(msg, e);
        }

        return valueList;
    }

}
