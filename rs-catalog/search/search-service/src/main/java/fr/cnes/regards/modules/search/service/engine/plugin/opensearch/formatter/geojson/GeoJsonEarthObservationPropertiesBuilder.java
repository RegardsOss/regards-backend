/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.geojson;

import com.google.common.collect.Sets;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.eo.EarthObservationAttribute;

import java.util.*;

/**
 * Build Earth Observation properties to add them on an OpenSearch GeoJSON Feature
 *
 * @author Léo Mieulet
 */
public class GeoJsonEarthObservationPropertiesBuilder {

    private GeoJsonEarthObservationPropertiesBuilder() {
    }

    public static Map<String, ?> buildProperties(Map<EarthObservationAttribute, Object> activeProperties) {
        Map<String, Object> nested = new HashMap<>();
        if (!activeProperties.isEmpty()) {
            handleAcquisitionAnglesNode(activeProperties, nested);
            handleAcquisitionInformationNode(activeProperties, nested);
            handleAcquisitionParametersNode(activeProperties, nested);
            handleProductInformationNode(activeProperties, nested);
            handleQualityInformationNode(activeProperties, nested);
            handleWaveLengthsNode(activeProperties, nested);
            handleRootNode(activeProperties, nested);
        }
        return nested;
    }

    /**
     * Handle $.properties.<property>
     */
    private static void handleRootNode(Map<EarthObservationAttribute, Object> activeProperties,
                                       Map<String, Object> nested) {

        HashSet<EarthObservationAttribute> relatedAttrs = getRootAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            nested.putAll(getGeoJSONProperties(activeProperties, relatedAttrs));
        }
    }

    private static HashSet<EarthObservationAttribute> getRootAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.TITLE,
                               EarthObservationAttribute.PARENT_IDENTIFIER,
                               EarthObservationAttribute.DOI,
                               EarthObservationAttribute.PRODUCTION_STATUS,
                               EarthObservationAttribute.AVAILABILITY_TIME);
    }

    /**
     * Handle $.properties.wavelengths[0]
     */
    private static void handleWaveLengthsNode(Map<EarthObservationAttribute, Object> activeProperties,
                                              Map<String, Object> nested) {
        HashSet<EarthObservationAttribute> relatedAttrs = getWaveLengthsAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            List<Object> waveLengthsNodes = new ArrayList<>();
            Map<String, Object> firstWaveLengthNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            waveLengthsNodes.add(firstWaveLengthNode);
            nested.put("wavelengths", waveLengthsNodes);
        }
    }

    private static HashSet<EarthObservationAttribute> getWaveLengthsAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.SPECTRAL_RANGE, EarthObservationAttribute.WAVELENGTH);
    }

    /**
     * Handle $.properties.qualityInformation
     */
    private static void handleQualityInformationNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                     Map<String, Object> nested) {

        HashSet<EarthObservationAttribute> relatedAttrs = getQualityInformationAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> qualityInformationNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            nested.put("qualityInformation", qualityInformationNode);
        }
    }

    private static HashSet<EarthObservationAttribute> getQualityInformationAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.PRODUCT_QUALITY_STATUS,
                               EarthObservationAttribute.PRODUCT_QUALITY_DEGRADATION_TAG);
    }

    /**
     * Handle $.properties.productInformation
     */
    private static void handleProductInformationNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                     Map<String, Object> nested) {

        HashSet<EarthObservationAttribute> relatedAttrs = getProductInformationAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> acquisitionAnglesNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            nested.put("productInformation", acquisitionAnglesNode);
        }

    }

    private static HashSet<EarthObservationAttribute> getProductInformationAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.ARCHIVING_CENTER,
                               EarthObservationAttribute.PRODUCT_VERSION,
                               EarthObservationAttribute.PROCESSOR_NAME,
                               EarthObservationAttribute.PROCESSING_CENTER,
                               EarthObservationAttribute.PROCESSING_MODE,
                               EarthObservationAttribute.PROCESSING_DATE,
                               EarthObservationAttribute.PROCESSING_LEVEL,
                               EarthObservationAttribute.PRODUCT_TYPE,
                               EarthObservationAttribute.COMPOSITE_TYPE,
                               EarthObservationAttribute.SNOW_COVER,
                               EarthObservationAttribute.CLOUD_COVER);
    }

    /**
     * Handle $.properties.acquisitionParameters
     */
    private static void handleAcquisitionParametersNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                        Map<String, Object> nested) {

        HashSet<EarthObservationAttribute> relatedAttrs = getAcquisitionParametersAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> acquisitionParametersNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            nested.put("acquisitionParameters", acquisitionParametersNode);
        }
    }

    private static HashSet<EarthObservationAttribute> getAcquisitionParametersAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.ANTENNA_LOOK_DIRECTION,
                               EarthObservationAttribute.ACQUISITION_TYPE,
                               EarthObservationAttribute.ACQUISITION_STATION,
                               EarthObservationAttribute.ACQUISITION_SUB_TYPE,
                               EarthObservationAttribute.START_TIME_FROM_ASCENDING_NODE,
                               EarthObservationAttribute.COMPLETION_TIME_FROM_ASCENDING_NODE);
    }

    /**
     * Handle $.properties.acquisitionInformation
     */
    private static void handleAcquisitionInformationNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                         Map<String, Object> nested) {
        HashSet<EarthObservationAttribute> relatedAttrs = getAcquisitionInformationAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            List<Object> acquisitionInformationNodes = new ArrayList<>();
            Map<String, Object> firstAcquisitionInformationNode = new HashMap<>();
            handleAcquisitionInformationInstrumentNode(activeProperties, firstAcquisitionInformationNode);
            handleAcquisitionInformationAcquisitionParametersNode(activeProperties, firstAcquisitionInformationNode);
            handleAcquisitionInformationPlatformNode(activeProperties, firstAcquisitionInformationNode);
            acquisitionInformationNodes.add(firstAcquisitionInformationNode);
            nested.put("acquisitionInformation", acquisitionInformationNodes);
        }
    }

    private static HashSet<EarthObservationAttribute> getAcquisitionInformationAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.DOPPLER_FREQUENCY,
                               EarthObservationAttribute.LOWEST_LOCATION,
                               EarthObservationAttribute.HIGHEST_LOCATION,
                               EarthObservationAttribute.SENSOR_MODE,
                               EarthObservationAttribute.INSTRUMENT,
                               EarthObservationAttribute.SWATH_IDENTIFIER,
                               EarthObservationAttribute.POLARISATION_MODE,
                               EarthObservationAttribute.POLARIZATION_CHANNELS,
                               EarthObservationAttribute.PLATFORM,
                               EarthObservationAttribute.PLATFORM_SERIAL_IDENTIFIER,
                               EarthObservationAttribute.SENSOR_TYPE,
                               EarthObservationAttribute.ORBIT_TYPE,
                               EarthObservationAttribute.ORBIT_NUMBER,
                               EarthObservationAttribute.RESOLUTION);
    }

    /**
     * Handle $.properties.acquisitionInformation.platform
     */
    private static void handleAcquisitionInformationPlatformNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                                 Map<String, Object> firstAcquisitionInformationNode) {
        HashSet<EarthObservationAttribute> relatedAttrs = getAcquisitionInformationPlatformAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> platformNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            firstAcquisitionInformationNode.put("platform", platformNode);
        }
    }

    private static HashSet<EarthObservationAttribute> getAcquisitionInformationPlatformAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.ORBIT_TYPE,
                               EarthObservationAttribute.PLATFORM,
                               EarthObservationAttribute.PLATFORM_SERIAL_IDENTIFIER);
    }

    /**
     * Handle $.properties.acquisitionInformation.instrument
     */
    private static void handleAcquisitionInformationInstrumentNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                                   Map<String, Object> firstAcquisitionInformationNode) {
        HashSet<EarthObservationAttribute> relatedAttrs = getAcquisitionInformationInstrumentAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> instrumentNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            firstAcquisitionInformationNode.put("instrument", instrumentNode);
        }
    }

    private static HashSet<EarthObservationAttribute> getAcquisitionInformationInstrumentAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.INSTRUMENT, EarthObservationAttribute.SENSOR_TYPE);
    }

    /**
     * Handle $.properties.acquisitionInformation.acquisitionParameters
     */
    private static void handleAcquisitionInformationAcquisitionParametersNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                                              Map<String, Object> firstAcquisitionInformationNode) {
        HashSet<EarthObservationAttribute> relatedAttrs = getAcquisitionInformationAcquisitionAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> acquisitionParametersNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            firstAcquisitionInformationNode.put("acquisitionParameters", acquisitionParametersNode);
        }
    }

    private static HashSet<EarthObservationAttribute> getAcquisitionInformationAcquisitionAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.DOPPLER_FREQUENCY,
                               EarthObservationAttribute.HIGHEST_LOCATION,
                               EarthObservationAttribute.LOWEST_LOCATION,
                               EarthObservationAttribute.ORBIT_NUMBER,
                               EarthObservationAttribute.POLARISATION_MODE,
                               EarthObservationAttribute.POLARIZATION_CHANNELS,
                               EarthObservationAttribute.SWATH_IDENTIFIER,
                               EarthObservationAttribute.SENSOR_MODE,
                               EarthObservationAttribute.RESOLUTION);
    }

    /**
     * Handle $.properties.acquisitionAngles
     */
    private static void handleAcquisitionAnglesNode(Map<EarthObservationAttribute, Object> activeProperties,
                                                    Map<String, Object> nested) {
        HashSet<EarthObservationAttribute> relatedAttrs = getAcquisitionAnglesAttributes();
        relatedAttrs.retainAll(activeProperties.keySet());
        if (!relatedAttrs.isEmpty()) {
            Map<String, Object> acquisitionAnglesNode = getGeoJSONProperties(activeProperties, relatedAttrs);
            nested.put("acquisitionAngles", acquisitionAnglesNode);
        }
    }

    private static HashSet<EarthObservationAttribute> getAcquisitionAnglesAttributes() {
        return Sets.newHashSet(EarthObservationAttribute.ILLUMINATION_AZIMUTH_ANGLE,
                               EarthObservationAttribute.ILLUMINATION_ELEVATION_ANGLE,
                               EarthObservationAttribute.ILLUMINATION_ZENITH_ANGLE,
                               EarthObservationAttribute.MINIMUM_INCIDENCE_ANGLE,
                               EarthObservationAttribute.MAXIMUM_INCIDENCE_ANGLE,
                               EarthObservationAttribute.INCIDENCE_ANGLE_VARIATION);
    }

    private static Map<String, Object> getGeoJSONProperties(Map<EarthObservationAttribute, Object> activeProperties,
                                                            Set<EarthObservationAttribute> nodeProperties) {
        Map<String, Object> nodeValues = new HashMap<>();
        for (EarthObservationAttribute attribute : nodeProperties) {
            String propertyKey = getGeoJSONPropertyKey(attribute);
            Object value = activeProperties.get(attribute);
            if (value != null) {
                nodeValues.put(propertyKey, value);
            }
        }
        return nodeValues;
    }

    /**
     * Retrieve the property key for a specific {@link EarthObservationAttribute}
     * As some attributes are renamed in the GeoJSON extension
     */
    private static String getGeoJSONPropertyKey(EarthObservationAttribute propertyName) {
        switch (propertyName) {
            case SENSOR_MODE:
                return "operationalMode";
            case INSTRUMENT:
                return "instrumentShortName";
            case PLATFORM:
                return "platformShortName";
            case WAVELENGTH:
                return "discreteWavelengths";
            case PRODUCTION_STATUS:
                return "production";
            case AVAILABILITY_TIME:
                return "available";
            default:
                return propertyName.toString();
        }
    }
}
