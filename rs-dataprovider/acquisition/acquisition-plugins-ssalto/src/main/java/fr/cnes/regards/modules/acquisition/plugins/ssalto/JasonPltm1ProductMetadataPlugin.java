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
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;

/**
 *
 * Class JasonPltm1ProductMetadataPlugin
 *
 * Plugin pour les produits PLTM1 Des missions JASONX
 *
 * @author Christophe Mertz
 */
public abstract class JasonPltm1ProductMetadataPlugin extends AbstractJasonDoris10ProductMetadataPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(JasonPltm1ProductMetadataPlugin.class);

    private static final String RADICAL = "RADICAL";

    /**
     *
     * Méthode abstraite permettant de fournir le prefixe des patterns de fichier lié à la mission JASON. Exemple :
     * "JA1" pour JASON1, "JA2" pour JASON2 ou "JA3" pour JASON3
     *
     * @return
     */
    protected abstract String getProjectPrefix();

    public JasonPltm1ProductMetadataPlugin() {
        super();
    }

    /**
     * Methode surchargee
     *
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.Jason2Doris10ProductMetadataPlugin#doCreateIndependantSpecificAttributes(java.util.Map,
     *      java.util.Map)
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        super.doCreateIndependantSpecificAttributes(pFileMap, pAttributeMap);
        registerRadicalAttribute(pFileMap, pAttributeMap);
    }

    /**
     * Methode surchargee
     *
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.Jason2Doris10ProductMetadataPlugin#init(java.lang.String)
     */
    @Override
    public void init(String pDataSetName) throws ModuleException {
        super.init(pDataSetName);
        patternd = Pattern.compile(getProjectPrefix() + "_PLTM1_P_.*_([0-9]{8}_[0-9]{6})__");
        patternp = Pattern.compile(getProjectPrefix()
                + "_PLTM1_P_.*_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})_([0-9]{8}_[0-9]{6})");
    }

    /**
     * Methode surchargee
     *
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.Jason2Doris10ProductMetadataPlugin#getCreationDateValue(java.util.Collection)
     */
    @Override
    protected List<Date> getCreationDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        for (File file : pSsaltoFileList) {
            String fileName = file.getName();
            SimpleDateFormat fileNameFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            try {
                if (matcherD.matches()) {
                    String dateStr = matcherD.group(1);
                    Date date = fileNameFormat.parse(dateStr);
                    valueList.add(date);
                } else if (matcherP.matches()) {
                    String dateStr = matcherP.group(1);
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

    /**
     * Methode surchargee
     *
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.Jason2Doris10ProductMetadataPlugin#getStopDateValue(java.util.Collection)
     */
    @Override
    protected List<Date> getStopDateValue(Collection<File> pSsaltoFileList) throws PluginAcquisitionException {
        List<Date> valueList = new ArrayList<>();
        for (File file : pSsaltoFileList) {
            String fileName = file.getName();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            try {
                if (matcherD.matches()) {
                    String dateStr = matcherD.group(1);
                    Date date = format.parse(dateStr);
                    valueList.add(date);
                } else if (matcherP.matches()) {
                    String dateStr = matcherP.group(3);
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

    /**
     *
     * Calcul de l'attribut RADICAL
     *
     * @param pSsaltoFileList
     * @return
     * @throws PluginAcquisitionException
     */
    protected List<String> getRadicalValue(Set<File> pSsaltoFileList) throws PluginAcquisitionException {

        List<String> valueList = new ArrayList<>();
        for (File file : pSsaltoFileList) {
            String fileName = file.getName();

            Matcher matcherD = patternd.matcher(fileName);
            Matcher matcherP = patternp.matcher(fileName);
            String radical = null;

            if (matcherP.matches()) {
                String fileNamePattern = getProperties().getFileNamePattern();

                // get the prefix
                String prefix = fileNamePattern.substring(0, fileNamePattern.indexOf("("));
                radical = prefix;

                // get the pid num
                Pattern patternFile = Pattern.compile(fileNamePattern);
                Matcher matcherFile = patternFile.matcher(fileName);

                if (matcherFile.matches()) {
                    radical += matcherFile.group(1);
                }

                // get the radical
                radical += "_" + matcherP.group(2);
                radical += "_" + matcherP.group(3);
                valueList.add(radical);
            } else {
                if (!matcherD.matches()) {
                    String msg = "filename does not match";
                    LOGGER.error(msg);
                    throw new PluginAcquisitionException(msg);
                }
            }
        }
        return valueList;
    }

    /**
     *
     * Ajout de l'attribut RADICAL à la map des attributs
     *
     * @param pFileMap
     * @param pAttributeMap
     * @throws PluginAcquisitionException
     */
    private void registerRadicalAttribute(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {
        LOGGER.info("START building attribute " + RADICAL);
        try {
            Attribute radicalAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_STRING, RADICAL,
                                                                          getRadicalValue(pFileMap.keySet()));
            if ((radicalAttribute.getValueList() != null) && (radicalAttribute.getValueList().size() != 0)) {
                registerAttribute(RADICAL, pAttributeMap, radicalAttribute);
                attributeValueMap_.put(RADICAL, radicalAttribute.getValueList());
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
