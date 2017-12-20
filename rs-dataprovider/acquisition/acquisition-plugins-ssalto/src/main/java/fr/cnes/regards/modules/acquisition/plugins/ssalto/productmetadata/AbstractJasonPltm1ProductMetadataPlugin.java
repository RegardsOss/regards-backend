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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;

/**
 * Plugin pour les produits PLTM1 des missions JASON1, JASON2 et JASON3.
 *
 * @author Christophe Mertz
 */
public abstract class AbstractJasonPltm1ProductMetadataPlugin extends AbstractJasonDoris10ProductMetadataPlugin {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJasonPltm1ProductMetadataPlugin.class);

    private static final String RADICAL = "RADICAL";

    public AbstractJasonPltm1ProductMetadataPlugin() {
        super();
    }

    /**
     *
     * Méthode abstraite permettant de fournir le prefixe des patterns de fichier lié à la mission JASON.
     * Exemple :
     * "JA1" pour JASON1, "JA2" pour JASON2 ou "JA3" pour JASON3
     *
     * @return
     */
    protected abstract String getProjectPrefix();

    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        super.doCreateIndependantSpecificAttributes(pFileMap, pAttributeMap);
        registerRadicalAttribute(pFileMap, pAttributeMap);
    }

    @Override
    public void loadDataSetConfiguration(String dataSetName) throws ModuleException {
        super.loadDataSetConfiguration(dataSetName);
        patternd = Pattern.compile(getProjectPrefix() + "_PLTM1_P_.*_([0-9]{8}_[0-9]{6})__");
        patternp = Pattern.compile(getProjectPrefix()
                + "_PLTM1_P_.*_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})");
    }

    @Override
    protected List<OffsetDateTime> getCreationDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            String dateStr;
            if (matcherD.matches()) {
                dateStr = matcherD.group(1);
            } else if (matcherP.matches()) {
                dateStr = matcherP.group(1);
            } else {
                LOGGER.error(MSG_ERR_FILENAME);
                throw new PluginAcquisitionException(MSG_ERR_FILENAME);
            }

            LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
            valueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        }
        return valueList;
    }

    @Override
    protected List<OffsetDateTime> getStopDateValue(Collection<File> files) throws PluginAcquisitionException {
        List<OffsetDateTime> valueList = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            String dateStr;

            if (matcherD.matches()) {
                dateStr = matcherD.group(1);
            } else if (matcherP.matches()) {
                dateStr = matcherP.group(3);
            } else {
                LOGGER.error(MSG_ERR_FILENAME);
                throw new PluginAcquisitionException(MSG_ERR_FILENAME);
            }

            LocalDateTime ldt = LocalDateTime.parse(dateStr, DATETIME_FORMATTER);
            valueList.add(OffsetDateTime.of(ldt, ZoneOffset.UTC));
        }
        return valueList;
    }

    /**
     *
     * Calcul de l'attribut RADICAL
     *
     * @param files
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<String> getRadicalValue(Set<File> files) throws PluginAcquisitionException {

        List<String> valueList = new ArrayList<>();
        for (File file : files) {
            String fileName = file.getName();

            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);

            if (matcherP.matches()) {
                String fileNamePattern = getProperties().getFileNamePattern();

                // get the prefix
                String prefix = fileNamePattern.substring(0, fileNamePattern.indexOf("("));
                StringBuilder strBuilder = new StringBuilder(prefix);

                // get the pid num
                Pattern patternFile = Pattern.compile(fileNamePattern);
                Matcher matcherFile = patternFile.matcher(fileName);

                if (matcherFile.matches()) {
                    strBuilder.append(matcherFile.group(1));
                }

                // get the radical
                strBuilder.append("_");
                strBuilder.append(matcherP.group(2));
                strBuilder.append("_");
                strBuilder.append(matcherP.group(3));
                valueList.add(strBuilder.toString());
            } else {
                if (!matcherD.matches()) {
                    LOGGER.error(MSG_ERR_FILENAME);
                    throw new PluginAcquisitionException(MSG_ERR_FILENAME);
                }
            }
        }
        return valueList;
    }

    /**
     *
     * Ajout de l'attribut RADICAL à la map des attributs
     *
     * @param fileMap
     * @param attributeMap
     * @throws PluginAcquisitionException
     */
    private void registerRadicalAttribute(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + RADICAL);

        try {
            Attribute radicalAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_STRING, RADICAL,
                                                                          getRadicalValue(fileMap.keySet()));
            if ((radicalAttribute.getValueList() != null) && (!radicalAttribute.getValueList().isEmpty())) {
                registerAttribute(attributeMap, RADICAL, radicalAttribute);
                attributeValueMap.put(RADICAL, radicalAttribute.getValueList());
            } else {
                LOGGER.info("Attribute " + RADICAL + " is not defined");
            }
        } catch (DomainModelException e) {
            String msg = "unable to create attribute" + RADICAL;
            throw new PluginAcquisitionException(msg, e);
        }

        LOGGER.info("END building attribute " + RADICAL);
    }

}
