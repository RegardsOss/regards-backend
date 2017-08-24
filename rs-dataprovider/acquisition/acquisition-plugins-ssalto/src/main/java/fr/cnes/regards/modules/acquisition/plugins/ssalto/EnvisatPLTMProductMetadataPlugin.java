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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.modules.acquisition.domain.model.Attribute;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeFactory;
import fr.cnes.regards.modules.acquisition.domain.model.AttributeTypeEnum;
import fr.cnes.regards.modules.acquisition.domain.model.CompositeAttribute;
import fr.cnes.regards.modules.acquisition.exception.DomainModelException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.exception.PluginAcquisitionException;
import fr.cnes.regards.modules.acquisition.plugins.ssalto.finder.AsciiFileFinder;

/**
 * Cette classe permet de calculer de maniere specifique pour les fichiers ENVISAT PLTM, les elements suivants :
 * <ul>
 * <li>GEO_COORDINATES/LONGITUDE_MIN</li>
 * <li>GEO_COORDINATES/LONGITUDE_MAX</li>
 * <li>GEO_COORDINATES/LATITUDE_MIN</li>
 * <li>GEO_COORDINATES/LATITUDE_MAX</li>
 * </ul>
 * 
 * @author CS
 * @version 1.4
 * @since 1.4
 */
public class EnvisatPLTMProductMetadataPlugin extends EnvisatProductMetadataPlugin {

    private static final String GEO_COORDINATES = "GEO_COORDINATES";

    private static final String LONGITUDE_MIN = "LONGITUDE_MIN";

    private static final String LONGITUDE_MAX = "LONGITUDE_MAX";

    private static final String LATITUDE_MIN = "LATITUDE_MIN";

    private static final String LATITUDE_MAX = "LATITUDE_MAX";

    private static final double DIX_MOINS_6 = 0.000001;

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvisatPLTMProductMetadataPlugin.class);

    private List<Double> longitudeMin_ = null;

    private List<Double> longitudeMax_ = null;

    private List<Double> latitudeMin_ = null;

    private List<Double> latitudeMax_ = null;

    /**
     * 
     * Calcul des attributs LONGITUDE_MIN, LONGITUDE_MAX, LATITUDE_MIN, LATITUDE_MAX
     * 
     * @see fr.cnes.regards.modules.acquisition.plugins.ssalto.GenericProductMetadataPlugin#doCreateIndependantSpecificAttributes(java.util.Map,
     *      java.util.Map)
     * @since 1.4
     */
    @Override
    protected void doCreateIndependantSpecificAttributes(Map<File, ?> pFileMap, Map<Integer, Attribute> pAttributeMap)
            throws PluginAcquisitionException {

        // Init composite attribute GEO_COORDINATES
        LOGGER.info("START building attribute " + GEO_COORDINATES);
        CompositeAttribute coordinates = new CompositeAttribute();
        coordinates.setName(GEO_COORDINATES);

        try {
            // Compute values
            computeLongitudeMin(pFileMap, attributeValueMap_);
            computeLongitudeMax(pFileMap, attributeValueMap_);
            computeLatitude(pFileMap, attributeValueMap_);

            // Add LONGITUDE_MIN attribute
            Attribute longitudeMinAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL,
                                                                               LONGITUDE_MIN, longitudeMin_);
            coordinates.addAttribute(longitudeMinAttribute);
            attributeValueMap_.put(LONGITUDE_MIN, longitudeMinAttribute.getValueList());

            // Add LONGITUDE_MAX attribute
            Attribute longitudeMaxAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL,
                                                                               LONGITUDE_MAX, longitudeMax_);
            coordinates.addAttribute(longitudeMaxAttribute);
            attributeValueMap_.put(LONGITUDE_MAX, longitudeMaxAttribute.getValueList());

            // Add LONGITUDE_MIN attribute
            Attribute latitudeMinAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL,
                                                                              LATITUDE_MIN, latitudeMin_);
            coordinates.addAttribute(latitudeMinAttribute);
            attributeValueMap_.put(LATITUDE_MIN, latitudeMinAttribute.getValueList());

            // Add LONGITUDE_MAX attribute
            Attribute latitudeMaxAttribute = AttributeFactory.createAttribute(AttributeTypeEnum.TYPE_REAL,
                                                                              LATITUDE_MAX, latitudeMax_);
            coordinates.addAttribute(latitudeMaxAttribute);
            attributeValueMap_.put(LATITUDE_MAX, latitudeMaxAttribute.getValueList());

        }
        catch (DomainModelException e) {
            String msg = "Unable to create attribute" + GEO_COORDINATES;
            LOGGER.error(msg);
            throw new PluginAcquisitionException(msg, e);
        }
        registerAttribute(GEO_COORDINATES, pAttributeMap, coordinates);
        LOGGER.info("END building attribute " + GEO_COORDINATES);
    }

    /***************************************************************************
     * COMPUTE ATTRIBUTE VALUES
     **************************************************************************/

    /**
     * Compute min longitude value
     * 
     * @param pFileMap
     * @param pAttributeValueMap
     * @throws PluginAcquisitionException
     * @since 1.4
     */
    private void computeLongitudeMin(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        longitudeMin_ = new ArrayList<>();

        AsciiFileFinder finder = new AsciiFileFinder();
        finder.setLinePattern("START_LONG=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        try {
            finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());
        }
        catch (Exception e) {
            throw new PluginAcquisitionException(e);
        }
        @SuppressWarnings("unchecked")
        List<Double> valueList = (List<Double>) finder.getValueList(pFileMap, pAttributeValueMap);
        // Compute values
        for (Double double1 : valueList) {
            Double longitude = longitudeToGreenwichCenterLongitude(double1);
            longitudeMin_.add(longitude);
        }
    }

    /**
     * Compute max longitude
     * 
     * @param pFileMap
     * @param pAttributeValueMap
     * @throws PluginAcquisitionException
     * @since 1.4
     */
    private void computeLongitudeMax(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        longitudeMax_ = new ArrayList<>();

        AsciiFileFinder finder = new AsciiFileFinder();
        finder.setLinePattern("STOP_LONG=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        try {
            finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());
        }
        catch (Exception e) {
            throw new PluginAcquisitionException(e);
        }
        @SuppressWarnings("unchecked")
        List<Double> valueList = (List<Double>) finder.getValueList(pFileMap, pAttributeValueMap);
        // Compute values
        for (Double double1 : valueList) {
            Double longitude = longitudeToGreenwichCenterLongitude(double1);
            longitudeMax_.add(longitude);
        }
    }

    /**
     * Calcule les latitudes
     * 
     * @param pFileMap
     * @param pAttributeValueMap
     * @throws PluginAcquisitionException
     * @since 1.4
     */
    private void computeLatitude(Map<File, ?> pFileMap, Map<String, List<? extends Object>> pAttributeValueMap)
            throws PluginAcquisitionException {
        latitudeMin_ = new ArrayList<>();
        latitudeMax_ = new ArrayList<>();

        // Get START_LAT
        AsciiFileFinder finder = new AsciiFileFinder();
        finder.setLinePattern("START_LAT=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        try {
            finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());
        }
        catch (Exception e) {
            throw new PluginAcquisitionException(e);
        }
        @SuppressWarnings("unchecked")
        List<Double> startLatList = (List<Double>) finder.getValueList(pFileMap, pAttributeValueMap);

        // Get STOP_LAT
        finder = new AsciiFileFinder();
        finder.setLinePattern("STOP_LAT=((-|\\+)[0-9]{10}).*");
        finder.setLineNumber("0");
        finder.addGroupNumber("1");
        try {
            finder.setValueType(AttributeTypeEnum.TYPE_REAL.toString());
        }
        catch (Exception e) {
            throw new PluginAcquisitionException(e);
        }
        @SuppressWarnings("unchecked")
        List<Double> stopLatList = (List<Double>) finder.getValueList(pFileMap, pAttributeValueMap);

        // Compute latitudes
        if (!(startLatList.isEmpty() || (stopLatList.isEmpty()))) {
            double startLat = Double.parseDouble(startLatList.get(0).toString());
            double stopLat = Double.parseDouble(stopLatList.get(0).toString());
            startLat = startLat * DIX_MOINS_6;
            stopLat = stopLat * DIX_MOINS_6;
            // Format
            if (startLat < stopLat) {
                latitudeMin_.add(formatCoordinate(startLat));
                latitudeMax_.add(formatCoordinate(stopLat));
            }
            else {
                latitudeMin_.add(formatCoordinate(stopLat));
                latitudeMax_.add(formatCoordinate(startLat));
            }
        }
        else {
            throw new PluginAcquisitionException("Unknown START_LAT or STOP_LAT in file");
        }
    }

    /***************************************************************************
     * TOOLS METHOD
     **************************************************************************/

    /**
     * Formate la longitude pour ENVISAT PLTM
     */
    private Double longitudeToGreenwichCenterLongitude(Object pValue) {
        // * 10-6
        double value = Double.parseDouble(pValue.toString());
        value = value * DIX_MOINS_6;
        // Transform in Greenwich Center Longitude
        if (value > 180) {
            value = -360 + value;
        }
        return formatCoordinate(value);
    }

    /**
     * Limite la precision a deux chiffres apres la virgule.
     * 
     * @param pValue
     * @return Formatted object
     * @since 1.4
     */
    public Double formatCoordinate(double pValue) {
        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);
        nf.setMaximumFractionDigits(2);
        String output = nf.format(pValue);
        return new Double(output);
    }
}
