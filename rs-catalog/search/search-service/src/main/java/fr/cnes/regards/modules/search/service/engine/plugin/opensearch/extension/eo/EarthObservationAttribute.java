package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.eo;/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.Arrays;
import java.util.Optional;

/**
 * Enum for all attributes that exists in the Earth Observation extension.
 *
 * @author Léo Mieulet
 */
public enum EarthObservationAttribute {
    PRODUCT_TYPE("productType"),

    CREATION_DATE("creationDate"),

    DOI("doi"),

    PLATFORM("platform"),

    PLATFORM_SERIAL_IDENTIFIER("platformSerialIdentifier"),

    INSTRUMENT("instrument"),

    SENSOR_TYPE("sensorType"),

    COMPOSITE_TYPE("compositeType"),

    PROCESSING_LEVEL("processingLevel"),

    ORBIT_TYPE("orbitType"),

    SPECTRAL_RANGE("spectralRange"),

    WAVELENGTH("wavelength"),

    HAS_SECURITY_CONSTRAINTS("hasSecurityConstraints"),

    DISSEMINATION("dissemination"),

    TITLE("title"),

    PARENT_IDENTIFIER("parentIdentifier"),

    PRODUCTION_STATUS("productionStatus"),

    ACQUISITION_TYPE("acquisitionType"),

    ORBIT_NUMBER("orbitNumber"),

    SWATH_IDENTIFIER("swathIdentifier"),

    CLOUD_COVER("cloudCover"),

    SNOW_COVER("snowCover"),

    LOWEST_LOCATION("lowestLocation"),

    HIGHEST_LOCATION("highestLocation"),

    PRODUCT_VERSION("productVersion"),

    PRODUCT_QUALITY_STATUS("productQualityStatus"),

    PRODUCT_QUALITY_DEGRADATION_TAG("productQualityDegradationTag"),

    PROCESSOR_NAME("processorName"),

    PROCESSING_CENTER("processingCenter"),

    PROCESSING_DATE("processingDate"),

    SENSOR_MODE("sensorMode"),

    ARCHIVING_CENTER("archivingCenter"),

    PROCESSING_MODE("processingMode"),

    AVAILABILITY_TIME("availabilityTime"),

    ACQUISITION_STATION("acquisitionStation"),

    ACQUISITION_SUB_TYPE("acquisitionSubType"),

    START_TIME_FROM_ASCENDING_NODE("startTimeFromAscendingNode"),

    COMPLETION_TIME_FROM_ASCENDING_NODE("completionTimeFromAscendingNode"),

    ILLUMINATION_AZIMUTH_ANGLE("illuminationAzimuthAngle"),

    ILLUMINATION_ZENITH_ANGLE("illuminationZenithAngle"),

    ILLUMINATION_ELEVATION_ANGLE("illuminationElevationAngle"),

    POLARISATION_MODE("polarisationMode"),

    POLARIZATION_CHANNELS("polarizationChannels"),

    ANTENNA_LOOK_DIRECTION("antennaLookDirection"),

    MINIMUM_INCIDENCE_ANGLE("minimumIncidenceAngle"),

    MAXIMUM_INCIDENCE_ANGLE("maximumIncidenceAngle"),

    DOPPLER_FREQUENCY("dopplerFrequency"),

    INCIDENCE_ANGLE_VARIATION("incidenceAngleVariation"),

    RESOLUTION("resolution");

    private final String attributeName;

    EarthObservationAttribute(String attributeName) {
        this.attributeName = attributeName;
    }

    public static boolean exists(String name) {
        return Arrays.stream(values()).map(EarthObservationAttribute::toString).anyMatch(name::equals);
    }

    public static Optional<EarthObservationAttribute> fromName(String name) {
        return Arrays.stream(values()).filter(attribute -> attribute.attributeName.equals(name)).findFirst();
    }

    @Override
    public String toString() {
        return attributeName;
    }
}
