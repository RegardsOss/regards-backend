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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;
import fr.cnes.regards.modules.acquisition.tools.RinexFileHelper;

@Plugin(description = "Cryosat2Doris10ProductMetadataPlugin", id = "Cryosat2Doris10ProductMetadataPlugin",
        version = "1.0.0", author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class Cryosat2Doris10ProductMetadataPlugin extends Cryosat2ProductMetadataPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(Cryosat2Doris10ProductMetadataPlugin.class);

    private static final String TIME_PERIOD = "TIME_PERIOD";

    private static final String START_DATE = "START_DATE";

    private static final String STOP_DATE = "STOP_DATE";

    private static final String CREATION_DATE = "FILE_CREATION_DATE";

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final DateTimeFormatter DATETIME_FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss");

    private static final Pattern CREATION_DATE_PATTERN = Pattern.compile(".* ([0-9]{8} [0-9]{6}) UTC.*");

    protected Pattern patternd;

    protected Pattern patternp;

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    public Cryosat2Doris10ProductMetadataPlugin() {
        super();
    }

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    /**
     * ajoute l'initialisation du filePattern des fichiers en fonction du filePattern generique
     */
    @Override
    public void loadDataSetConfiguration(String dataSetName) throws ModuleException {
        super.loadDataSetConfiguration(dataSetName);
        patternd = Pattern.compile(".*[A-Z]([0-9]{8}_[0-9]{6})$");
        patternp = Pattern.compile(".*[A-Z]([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})$");
    }

    /**
     * cree les attributs TIME_PERIOD et FILE_CREATION_DATE
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        registerTimePeriodAttributes(pFileMap, attributeMap);
        registerFileCreationDateAttribute(pFileMap, attributeMap);
    }

    /**
     * Add the START_DATE and the STOP_DATE attributs
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

        registerAttribute(TIME_PERIOD, attributeMap, timePeriodAttribute);

        LOGGER.info("END building attribute " + TIME_PERIOD);
    }

    /**
     * Add the CREATION_DATE attribut
     * @param fileMap
     * @param attributeMap
     * @throws PluginAcquisitionException
     */
    private void registerFileCreationDateAttribute(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + CREATION_DATE);

        try {
            Attribute fileCreationDateAttribute = AttributeFactory
                    .createAttribute(AttributeTypeEnum.TYPE_DATE_TIME, CREATION_DATE,
                                     getCreationDateValue(fileMap.keySet()));
            registerAttribute(CREATION_DATE, attributeMap, fileCreationDateAttribute);
            attributeValueMap.put(CREATION_DATE, fileCreationDateAttribute.getValueList());
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + CREATION_DATE;
            throw new PluginAcquisitionException(msg, e);
        }

        LOGGER.info("END building attribute " + CREATION_DATE);
    }

    /**
     * Get the START_DATE
     * @param files
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<OffsetDateTime> getStartDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            String dateStr;

            if (matcherD.matches()) {
                dateStr = matcherD.group(1);
            } else if (matcherP.matches()) {
                dateStr = matcherP.group(2);
            } else {
                String msg = "filename does not match";
                LOGGER.error(msg);
                throw new PluginAcquisitionException(msg);
            }

            LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
            valueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        }
        return valueList;
    }

    /**
     * Get the STOP_DATE
     * @param files
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<OffsetDateTime> getStopDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        int n = 0;
        for (File file : files) {
            String fileName = file.getName();
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            if (matcherD.matches()) {
                OffsetDateTime date = (OffsetDateTime) attributeValueMap.get(START_DATE).get(n);
                valueList.add(date.plusSeconds(86400));
            } else if (matcherP.matches()) {
                String dateStr = matcherP.group(3);
                LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
                valueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
            } else {
                String msg = "filename does not match";
                LOGGER.error(msg);
                throw new PluginAcquisitionException(msg);
            }
            n++;
        }
        return valueList;
    }

    /**
     * Get the CREATION_DATE
     * @param files
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<OffsetDateTime> getCreationDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            if (matcherD.matches()) {
                // go to search into file using RINExFileHelper
                RinexFileHelper helper = new RinexFileHelper(file);
                Pattern cdPattern = CREATION_DATE_PATTERN;
                String dateStr = helper.getValue(2, cdPattern, 1);
                LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FILE_FORMATTER);
                valueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
            } else if (matcherP.matches()) {
                String dateStr = matcherP.group(1);
                LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
                valueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
            } else {
                String msg = "filename does not match";
                LOGGER.error(msg);
                throw new PluginAcquisitionException(msg);
            }
        }
        return valueList;
    }
}
