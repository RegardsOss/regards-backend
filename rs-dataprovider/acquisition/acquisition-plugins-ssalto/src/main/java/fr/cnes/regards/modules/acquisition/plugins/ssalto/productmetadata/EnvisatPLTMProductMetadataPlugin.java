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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.finder.AsciiFileFinder;
import fr.cnes.regards.modules.acquisition.plugins.properties.PluginsRepositoryProperties;

/**
 * Cette classe permet de calculer de maniere specifique pour les fichiers ENVISAT PLTM, les elements suivants :
 * <ul>
 * <li>GEO_COORDINATES/LONGITUDE_MIN</li>
 * <li>GEO_COORDINATES/LONGITUDE_MAX</li>
 * <li>GEO_COORDINATES/LATITUDE_MIN</li>
 * <li>GEO_COORDINATES/LATITUDE_MAX</li>
 * </ul>
 * 
 * @author Christophe Mertz
 */
@Plugin(description = "EnvisatPLTMProductMetadataPlugin", id = "EnvisatPLTMProductMetadataPlugin", version = "1.0.0",
        author = "REGARDS Team", contact = "regards@c-s.fr", licence = "LGPLv3.0", owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class EnvisatPLTMProductMetadataPlugin extends EnvisatProductMetadataPlugin {

    @Autowired
    private PluginsRepositoryProperties pluginsRepositoryProperties;

    @Override
    protected PluginsRepositoryProperties getPluginsRepositoryProperties() {
        return pluginsRepositoryProperties;
    }

    private static final String GEO_COORDINATES = "GEO_COORDINATES";

    private static final String LONGITUDE_MIN = "LONGITUDE_MIN";

    private static final String LONGITUDE_MAX = "LONGITUDE_MAX";

    private static final String LATITUDE_MIN = "LATITUDE_MIN";

    private static final String LATITUDE_MAX = "LATITUDE_MAX";

    private static final double DIX_MOINS_6 = 0.000001;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvisatPLTMProductMetadataPlugin.class);

    private List<Double> longitudeMin = null;

    private List<Double> longitudeMax = null;

    private List<Double> latitudeMin = null;

    private List<Double> latitudeMax = null;

    /**
     * 
     * Calcul des attributs LONGITUDE_MIN, LONGITUDE_MAX, LATITUDE_MIN, LATITUDE_MAX
     * 
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> fileMap, Map<Integer, Attribute> attributeMap)
            throws PluginAcquisitionException {

        // Init composite attribute GEO_COORDINATES
        LOGGER.info("START building attribute " + GEO_COORDINATES);
        CompositeAttribute coordinates = new CompositeAttribute();
        coordinates.setName(GEO_COORDINATES);

        try {
            // Compute values
            computeLongitudeMin(fileMap, attributeValueMap);
            computeLongitudeMax(fileMap, attributeValueMap);
            computeLatitude(fileMap, attributeValueMap);

            // Add LONGITUDE_MIN attribute
            Attribute longitudeMinAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL,
                                                                               LONGITUDE_MIN, longitudeMin);
            coordinates.addAttribute(longitudeMinAttribute);
            attributeValueMap.put(LONGITUDE_MIN, longitudeMinAttribute.getValueList());

            // Add LONGITUDE_MAX attribute
            Attribute longitudeMaxAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL,
                                                                               LONGITUDE_MAX, longitudeMax);
            coordinates.addAttribute(longitudeMaxAttribute);
            attributeValueMap.put(LONGITUDE_MAX, longitudeMaxAttribute.getValueList());

            // Add LONGITUDE_MIN attribute
            Attribute latitudeMinAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL, LATITUDE_MIN,
                                                                              latitudeMin);
            coordinates.addAttribute(latitudeMinAttribute);
            attributeValueMap.put(LATITUDE_MIN, latitudeMinAttribute.getValueList());

            // Add LONGITUDE_MAX attribute
            Attribute latitudeMaxAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL, LATITUDE_MAX,
                                                                              latitudeMax);
            coordinates.addAttribute(latitudeMaxAttribute);
            attributeValueMap.put(LATITUDE_MAX, latitudeMaxAttribute.getValueList());

        } catch (DomainModelException e) {
            String msg = "Unable to create attribute" + GEO_COORDINATES;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg, e);
        }
        registerAttribute(GEO_COORDINATES, attributeMap, coordinates);
        LOGGER.info("END building attribute " + GEO_COORDINATES);
    }

    /***************************************************************************
     * COMPUTE ATTRIBUTE VALUES
     **************************************************************************/

    /**
     * Compute min longitude value
     * 
     * @param fileMap
     * @param attributeValueMap
     * @throws PluginAcquisitionException
     */
    private void computeLongitudeMin(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        longitudeMin = new ArrayList<>();

        AsciiFileFinder finder = new AsciiFileFinder();
        finder.setLinePattern("START_LONG=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());

        @SuppressWarnings("unchecked")
        List<Double> valueList = (List<Double>) finder.getValueList(fileMap, attributeValueMap);
        // Compute values
        for (Double double1 : valueList) {
            Double longitude = longitudeToGreenwichCenterLongitude(double1);
            longitudeMin.add(longitude);
        }
    }

    /**
     * Compute max longitude
     * 
     * @param fileMap
     * @param attributeValueMap
     * @throws PluginAcquisitionException
     */
    private void computeLongitudeMax(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        longitudeMax = new ArrayList<>();

        AsciiFileFinder finder = new AsciiFileFinder();
        finder.setLinePattern("STOP_LONG=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());

        @SuppressWarnings("unchecked")
        List<Double> valueList = (List<Double>) finder.getValueList(fileMap, attributeValueMap);
        // Compute values
        for (Double double1 : valueList) {
            Double longitude = longitudeToGreenwichCenterLongitude(double1);
            longitudeMax.add(longitude);
        }
    }

    /**
     * Calcule les latitudes
     * 
     * @param fileMap
     * @param attributeValueMap
     * @throws PluginAcquisitionException
     */
    private void computeLatitude(Map<File, ?> fileMap, Map<String, List<? extends Object>> attributeValueMap)
            throws PluginAcquisitionException {
        latitudeMin = new ArrayList<>();
        latitudeMax = new ArrayList<>();

        // Get START_LAT
        AsciiFileFinder finder = new AsciiFileFinder();
        finder.setLinePattern("START_LAT=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());

        @SuppressWarnings("unchecked")
        List<Double> startLatList = (List<Double>) finder.getValueList(fileMap, attributeValueMap);

        // Get STOP_LAT
        finder = new AsciiFileFinder();
        finder.setLinePattern("STOP_LAT=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());

        @SuppressWarnings("unchecked")
        List<Double> stopLatList = (List<Double>) finder.getValueList(fileMap, attributeValueMap);

        // Compute latitudes
        if (!(startLatList.isEmpty() || (stopLatList.isEmpty()))) {
            double startLat = Double.parseDouble(startLatList.get(0).toString());
            double stopLat = Double.parseDouble(stopLatList.get(0).toString());
            startLat = startLat * DIX_MOINS_6;
            stopLat = stopLat * DIX_MOINS_6;
            // Format
            if (startLat < stopLat) {
                latitudeMin.add(formatCoordinate(startLat));
                latitudeMax.add(formatCoordinate(stopLat));
            } else {
                latitudeMin.add(formatCoordinate(stopLat));
                latitudeMax.add(formatCoordinate(startLat));
            }
        } else {
            throw new PluginAcquisitionException("Unknown START_LAT or STOP_LAT in file");
        }
    }

    /***************************************************************************
     * TOOLS METHOD
     **************************************************************************/

    /**
     * Formate la longitude pour ENVISAT PLTM
     */
    private Double longitudeToGreenwichCenterLongitude(Object newValue) {
        // * 10-6
        double value = Double.parseDouble(newValue.toString());
        value = value * DIX_MOINS_6;
        // Transform in Greenwich Center Longitude
        if (value > 180) {
            value = -360 + value;
        }
        return formatCoordinate(value);
    }

    /**
     * Limite la precision a deux chiffres apres la virgule
     * 
     * @param value
     * @return Formatted object
     */
    public Double formatCoordinate(double value) {
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        String output = nf.format(value);
        return new Double(output);
    }
}
