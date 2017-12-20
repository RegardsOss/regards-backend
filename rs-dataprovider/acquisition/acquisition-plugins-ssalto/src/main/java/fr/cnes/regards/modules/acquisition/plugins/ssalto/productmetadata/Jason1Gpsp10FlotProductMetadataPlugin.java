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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.productmetadata;

import java.io.File;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.tools.RinexFileHelper;

/**
 * plugin specifiques au donnees jason1 Gpsp10Flot les noms des fichiers ont deux formes bien distinctes et ne peuvent
 * pas etre resolues juste par le fichier de configuration. L' attribut traite specifiquement est le TIME_PERIOD
 * 
 * @author Christophe Mertz
 * 
 */
@Plugin(description = "Jason1Gpsp10FlotProductMetadataPlugin", id = "Jason1Gpsp10FlotProductMetadataPlugin",
        version = "1.0.0", author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Jason1Gpsp10FlotProductMetadataPlugin extends Jason1ProductMetadataPlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Jason1Gpsp10FlotProductMetadataPlugin.class);

    private static final String TIME_PERIOD = "TIME_PERIOD";

    private static final String START_DATE = "START_DATE";

    private static final String STOP_DATE = "STOP_DATE";

    protected static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected Pattern patternd;

    protected Pattern patternp;

    public Jason1Gpsp10FlotProductMetadataPlugin() {
        super();
    }

    /**
     * ajoute l'initialisation du filePattern des fichiers en fonction du filePattern generique
     */
    @Override
    public void loadDataSetConfiguration(String dataSetName) throws ModuleException {
        super.loadDataSetConfiguration(dataSetName);
        String fileNamePattern = getProperties().getFileNamePattern();
        String prefix = fileNamePattern.substring(0, fileNamePattern.indexOf("("));
        patternd = Pattern.compile(prefix + "[A-Z]([0-9]{8}_[0-9]{6})$");
        patternp = Pattern.compile(prefix + "[A-Z]([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})$");
    }

    /**
     * cree les attributs time_period et file_creation_date
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        registerTimePeriodAttributes(fileMap, attributeMap);
    }

    /**
     * 
     * @param fileMap
     * @param attributeMap
     * @throws PluginAcquisitionException
     */
    private void registerTimePeriodAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + TIME_PERIOD);

        CompositeAttribute timePeriodAttribute = new CompositeAttribute();
        timePeriodAttribute.setName(TIME_PERIOD);
        try {
            Attribute startDateAttribute = AttributeFactory
                    .createAttribute(AttributeTypeEnum.TYPE_DATE_TIME, START_DATE, getStartDateValue(fileMap.keySet()));
            timePeriodAttribute.addAttribute(startDateAttribute);
            attributeValueMap.put(START_DATE, startDateAttribute.getValueList());

            Attribute stopDateAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_DATE_TIME, STOP_DATE,
                                                                           getStopDateValue(fileMap.keySet()));
            timePeriodAttribute.addAttribute(stopDateAttribute);
            attributeValueMap.put(STOP_DATE, stopDateAttribute.getValueList());
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + TIME_PERIOD;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg, e);
        }

        registerAttribute(attributeMap, TIME_PERIOD, timePeriodAttribute);

        LOGGER.info("END building attribute " + TIME_PERIOD);
    }

    /**
     * Get the START_DATE value to a set of {@link File}
     * @param files a set of {@link File}
     * @return valueList the START_DATE value of each {@link File}
     * @throws PluginAcquisitionException
     */
    protected List<OffsetDateTime> getStartDateValue(Collection<File> files) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : files) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMinValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue > valueRead) {
                longValue = valueRead;
            }
        }
        List<OffsetDateTime> valueList = new ArrayList<>();
        Date newDate = new Date(longValue);
        valueList.add(OffsetDateTime.ofInstant(newDate.toInstant(), ZoneId.of("UTC")));
        return valueList;
    }

    /**
     * Get the STOP_DATE value to a set of {@link File}
     * @param files a set of {@link File}
     * @return valueList the STOP_DATE value of each {@link File}
     * @throws PluginAcquisitionException
     */
    protected List<OffsetDateTime> getStopDateValue(Collection<File> files) throws PluginAcquisitionException {
        long longValue = 0;
        for (File file : files) {
            RinexFileHelper helper = new RinexFileHelper(file);
            long valueRead = helper.getBlocMeasureDateInterval().getMaxValue();
            if (longValue == 0) {
                longValue = valueRead;
            } else if (longValue < valueRead) {
                longValue = valueRead;
            }
        }
        List<OffsetDateTime> valueList = new ArrayList<>();
        Date newDate = new Date(longValue);
        valueList.add(OffsetDateTime.ofInstant(newDate.toInstant(), ZoneId.of("UTC")));
        return valueList;
    }

    /**
     * Get the START_DATE value to a set of {@link File}
     * @param files a set of {@link File}
     * @return valueList the START_DATE value of each {@link File}
     * @throws PluginAcquisitionException a file name does not match the expected {@link Pattern} 
     */
    protected List<OffsetDateTime> getCreationDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        OffsetDateTime creationDate = null;
        for (File file : files) {
            RinexFileHelper helper = new RinexFileHelper(file);
            Pattern creationDatePattern = Pattern
                    .compile(".* ([\\d]{4}-[\\d]{2}-[\\d]{2} [\\d]{2}:[\\d]{2}:[\\d]{2}) .*");
            String dateStr = helper.getValue(2, creationDatePattern, 1);
            LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
            OffsetDateTime dateRead = OffsetDateTime.of(ldt, ZoneOffset.UTC);

            if (creationDate == null) {
                creationDate = dateRead;
            } else if (creationDate.isAfter(dateRead)) {
                creationDate = dateRead;
            }
        }
        valueList.add(creationDate);

        return valueList;
    }

}
