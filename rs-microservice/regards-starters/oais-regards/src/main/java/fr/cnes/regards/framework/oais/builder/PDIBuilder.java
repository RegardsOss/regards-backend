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
package fr.cnes.regards.framework.oais.builder;

import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Map;

import javax.annotation.Nullable;

import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.Event;
import fr.cnes.regards.framework.oais.PreservationDescriptionInformation;

/**
 *
 * Preservation Description Information Builder
 * @author Marc Sordi
 *
 */
public class PDIBuilder implements IOAISBuilder<PreservationDescriptionInformation> {

    private final PreservationDescriptionInformation pdi = new PreservationDescriptionInformation();

    @Override
    public PreservationDescriptionInformation build() {
        return pdi;
    }

    /**
     * Add <b>optional</b> context informations
     * @param contextInformations free context informations
     */
    public void addContextInformations(Map<String, Object> contextInformations) {
        Assert.notEmpty(contextInformations, "At least one information is required. Do not call this method otherwise");
        pdi.getContextInformation().putAll(contextInformations);
    }

    /**
     * * Add an <b>optional</b> context information
     * @param key information key
     * @param value information
     */
    public void addContextInformation(String key, Object value) {
        Assert.hasLength(key, "Context information key is required");
        Assert.notNull(value, "Context information value is required");
        pdi.getContextInformation().put(key, value);
    }

    /**
     * Set <b>required</b> provenance information
     * @param facility required facility
     * @param additional optional additional information (may be null)
     */
    public void setProvenanceInformation(String facility, @Nullable Map<String, Object> additional) {
        Assert.hasLength(facility, "Facility is required");
        pdi.getProvenanceInformation().setFacility(facility);

        if (additional != null) {
            pdi.getProvenanceInformation().getAdditional().putAll(additional);
        }
    }

    /**
     * Alias for {@link PDIBuilder#setProvenanceInformation(String, Map)} (no additional)
     * @param facility required facility
     */
    public void setProvenanceInformation(String facility) {
        setProvenanceInformation(facility, null);
    }

    /**
     * Add information object events
     * @param events events to add
     */
    public void addProvenanceInformationEvents(Event... events) {
        Assert.notEmpty(events, "At least one event is required if this method is called");
        for (Event event : events) {
            Assert.hasLength(event.getComment(), "Event comment is required");
            Assert.notNull(event.getDate(), "Event date is required");
            pdi.getProvenanceInformation().getHistory().add(event);
        }
    }

    /**
     * Add an information object event
     * @param optional type event type key (may be null)
     * @param comment event comment
     * @param date event date
     */
    public void addProvenanceInformationEvent(@Nullable String type, String comment, OffsetDateTime date) {
        Event event = new Event();
        event.setType(type);
        event.setComment(comment);
        event.setDate(date);
        addProvenanceInformationEvents(event);
    }

    /**
     * Add an information object event
     * @param comment event comment
     * @param date event date
     */
    public void addProvenanceInformationEvent(String comment, OffsetDateTime date) {
        addProvenanceInformationEvent(null, comment, date);
    }

    /**
     * Add an information object event
     * @param comment event comment
     */
    public void addProvenanceInformationEvent(String comment) {
        addProvenanceInformationEvent(null, comment, OffsetDateTime.now());
    }

    /**
     * Set <b>required</b> fixity information
     * @param checksum file checksum
     * @param algorithm related checksum algorithm (All available {@link MessageDigest} algorithm can be used)
     * @param fileSize file size (may be null)
     */
    public void setFixityInformation(String checksum, String algorithm, Long fileSize) {
        Assert.hasLength(checksum, "Checksum is required");
        Assert.hasLength(algorithm, "Checksum algorithm is required");
        pdi.getFixityInformation().setChecksum(checksum);
        pdi.getFixityInformation().setAlgorithm(algorithm);
        pdi.getFixityInformation().setFileSize(fileSize);
    }

    /**
     * Alias for {@link PDIBuilder#setFixityInformation(String, String, Long)} (no fileSize)
     * @param checksum file checksum
     * @param algorithm related checksum algorithm (All available {@link MessageDigest} algorithm can be used)
     *
     */
    public void setFixityInformation(String checksum, String algorithm) {
        setFixityInformation(checksum, algorithm, null);
    }

    /**
     * Set <b>required</b> access right information
     * @param publisherDID system unique identifier (i.e. REGARDS ip_id)
     * @param publisherID supplyer unique identifier (i.e. sip_id)
     * @param dataRights secure key
     * @param publicReleaseDate optional public release date (may be null)
     */
    public void setAccessRightInformation(String publisherDID, String publisherID, String dataRights,
            @Nullable OffsetDateTime publicReleaseDate) {
        Assert.hasLength(publisherDID, "Publisher DID is required");
        Assert.hasLength(publisherID, "Publisher ID is required");
        Assert.hasLength(dataRights, "Data rights is required");
        pdi.getAccesRightInformation().setPublisherDID(publisherDID);
        pdi.getAccesRightInformation().setPublisherID(publisherID);
        pdi.getAccesRightInformation().setDataRights(dataRights);
        pdi.getAccesRightInformation().setPublicReleaseDate(publicReleaseDate);
    }

    /**
     * Alias for {@link PDIBuilder#setAccessRightInformation(String, String, String, OffsetDateTime)} (no public release
     * date)
     * @param publisherDID system unique identifier (i.e. REGARDS ip_id)
     * @param publisherID supplyer unique identifier (i.e. sip_id)
     * @param dataRights secure key
     */
    public void setAccessRightInformation(String publisherDID, String publisherID, String dataRights) {
        setAccessRightInformation(publisherDID, publisherID, dataRights, null);
    }
}
